package com.clothingretail.auth.service;

import com.clothingretail.auth.AuthResult;
import com.clothingretail.auth.OtpChannel;
import com.clothingretail.auth.PendingRegistration;
import com.clothingretail.auth.RefreshToken;
import com.clothingretail.auth.Role;
import com.clothingretail.auth.RoleName;
import com.clothingretail.auth.User;
import com.clothingretail.auth.dto.AdminStaffRow;
import com.clothingretail.auth.dto.AuthResponse;
import com.clothingretail.auth.dto.CreateAdminRequest;
import com.clothingretail.auth.dto.LoginRequest;
import com.clothingretail.auth.dto.RegisterRequest;
import com.clothingretail.auth.dto.ResendOtpRequest;
import com.clothingretail.auth.dto.TokenResponse;
import com.clothingretail.auth.dto.UpdateProfileRequest;
import com.clothingretail.auth.dto.UserSummary;
import com.clothingretail.auth.dto.VerificationStatusResponse;
import com.clothingretail.auth.dto.VerifyOtpRequest;
import com.clothingretail.auth.repository.PendingRegistrationRepository;
import com.clothingretail.auth.repository.RefreshTokenRepository;
import com.clothingretail.auth.repository.RoleRepository;
import com.clothingretail.auth.repository.UserRepository;
import com.clothingretail.common.ConflictException;
import com.clothingretail.common.NotFoundException;
import com.clothingretail.customer.CustomerProfile;
import com.clothingretail.customer.repository.CustomerProfileRepository;
import com.clothingretail.notification.NotificationSettings;
import com.clothingretail.notification.repository.NotificationSettingsRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import lombok.extern.log4j.Log4j2;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Log4j2
public class AuthServiceImpl implements AuthService {

    /** How long an unfinished signup survives before PendingRegistrationCleanupJob reclaims it. */
    private static final long PENDING_REGISTRATION_TTL_SECONDS = 24L * 60 * 60;

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final CustomerProfileRepository customerProfileRepository;
    private final PendingRegistrationRepository pendingRegistrationRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final OtpService otpService;
    private final NotificationSettingsRepository notificationSettingsRepository;
    private final ModulePermissionService modulePermissionService;

    public AuthServiceImpl(
            UserRepository userRepository,
            RoleRepository roleRepository,
            RefreshTokenRepository refreshTokenRepository,
            CustomerProfileRepository customerProfileRepository,
            PendingRegistrationRepository pendingRegistrationRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            OtpService otpService,
            NotificationSettingsRepository notificationSettingsRepository,
            ModulePermissionService modulePermissionService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.customerProfileRepository = customerProfileRepository;
        this.pendingRegistrationRepository = pendingRegistrationRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.otpService = otpService;
        this.notificationSettingsRepository = notificationSettingsRepository;
        this.modulePermissionService = modulePermissionService;
    }

    /**
     * Read fresh on every register()/verifyOtp() call (not cached, not fixed at startup) so an
     * admin flipping a switch in the Notifications module (see NotificationSettingsService) takes
     * effect immediately for the very next signup - no restart needed.
     */
    private NotificationSettings loadNotificationSettings() {
        return notificationSettingsRepository.findById(NotificationSettings.SINGLETON_ID)
                .orElseThrow(() -> {
                    log.error("[1969] Singleton notification_settings row (id={}) is missing", NotificationSettings.SINGLETON_ID);
                    return new IllegalStateException(
                            "Singleton notification_settings row (id=1) is missing - this is a startup-time misconfiguration, "
                                    + "check that V16__notification_settings.sql ran");
                });
    }

    /**
     * A real `users` row is NEVER created here unless nothing needs verifying at all - see
     * {@link #buildVerificationResult}, the only place a {@link PendingRegistration} gets promoted
     * into a real account, once every required OTP channel is confirmed. An abandoned signup that
     * never finishes verification therefore leaves no trace in `users` - only a row in
     * pending_registrations, reclaimed by {@link PendingRegistrationCleanupJob}. Mobile OTP is only
     * triggered when a number was given (mobileNumber stays optional at signup - see
     * RegisterRequest).
     */
    @Transactional
    @Override
    public AuthResult<VerificationStatusResponse> register(RegisterRequest request) {
        log.info("[1000] Registration attempt for email={}", request.email());
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            log.error("[1001] Registration failed, email already exists: email={}", request.email());
            throw new ConflictException("An account with this email already exists");
        }

        NotificationSettings notificationSettings = loadNotificationSettings();
        boolean hasMobile = request.mobileNumber() != null && !request.mobileNumber().isBlank();
        boolean needsEmailOtp = notificationSettings.isEmailVerificationEnabled();
        boolean needsMobileOtp = notificationSettings.isMobileVerificationEnabled() && hasMobile;

        if (!needsEmailOtp && !needsMobileOtp) {
            User user = createVerifiedUser(request);
            AuthResult<AuthResponse> tokens = issueTokens(user);
            log.info("[1005] Registration completed immediately, no verification required userId={}", user.getId());
            return new AuthResult<>(new VerificationStatusResponse(true, null, false, false, tokens.body()), tokens.rawRefreshToken());
        }

        // Re-submitting the same (still-pending) email reuses its existing row rather than
        // erroring or creating a duplicate - a lost/expired code shouldn't force starting over,
        // and any channel already confirmed on it stays confirmed.
        PendingRegistration registration = pendingRegistrationRepository.findByEmailIgnoreCase(request.email())
                .orElseGet(PendingRegistration::new);
        boolean mobileChanged = !Objects.equals(registration.getMobileNumber(), request.mobileNumber());
        registration.setFullName(request.fullName());
        registration.setEmail(request.email());
        registration.setMobileNumber(request.mobileNumber());
        registration.setPasswordHash(passwordEncoder.encode(request.password()));
        registration.setExpiresAt(Instant.now().plusSeconds(PENDING_REGISTRATION_TTL_SECONDS));
        if (!needsEmailOtp) {
            registration.setEmailVerified(true);
        }
        if (!needsMobileOtp) {
            registration.setMobileVerified(true);
        } else if (mobileChanged) {
            // A different number than what was previously (maybe already) verified on this row -
            // that prior verification doesn't carry over to a number that was never confirmed.
            registration.setMobileVerified(false);
        }
        registration = pendingRegistrationRepository.save(registration);
        log.info("[1003] Pending registration saved id={} email={}", registration.getId(), registration.getEmail());

        if (needsEmailOtp && !registration.isEmailVerified()) {
            otpService.generateAndSend(registration, OtpChannel.EMAIL);
        }
        if (needsMobileOtp && !registration.isMobileVerified()) {
            otpService.generateAndSend(registration, OtpChannel.MOBILE);
        }

        AuthResult<VerificationStatusResponse> result = buildVerificationResult(registration, notificationSettings);
        log.info("[1005] Registration pending verification registrationId={}", registration.getId());
        return result;
    }

    /**
     * Confirms one OTP channel. Once every channel this signup actually needs (per the current
     * config toggles) is verified, {@link #buildVerificationResult} promotes it into a real account
     * and tokens are issued in the same call - there's no separate "now log in" step, matching how
     * registration used to work before OTP existed.
     */
    // noRollbackFor is required here: OtpService.verify() persists the incremented attempt count
    // even on a wrong code (that's the whole point - it's what eventually locks the code out
    // after maxAttempts), but that write happens inside THIS method's transaction. Throwing
    // BadCredentialsException below to report "wrong code" to the caller would otherwise roll
    // the whole transaction back by Spring's default behavior - silently undoing the attempt
    // count on every single failed try and defeating the lockout entirely.
    @Transactional(noRollbackFor = BadCredentialsException.class)
    @Override
    public AuthResult<VerificationStatusResponse> verifyOtp(VerifyOtpRequest request) {
        log.info("[1959] OTP verify attempt registrationId={} channel={}", request.registrationId(), request.channel());
        PendingRegistration registration = pendingRegistrationRepository.findById(request.registrationId())
                .orElseThrow(() -> {
                    log.error("[1960] OTP verify failed, pending registration not found registrationId={}", request.registrationId());
                    return new NotFoundException("Registration not found or already completed: " + request.registrationId());
                });
        boolean matched = otpService.verify(registration, request.channel(), request.code());
        if (!matched) {
            log.error("[1961] OTP verify rejected, incorrect code registrationId={} channel={}", registration.getId(), request.channel());
            throw new BadCredentialsException("Incorrect code");
        }
        if (request.channel() == OtpChannel.EMAIL) {
            registration.setEmailVerified(true);
        } else {
            registration.setMobileVerified(true);
        }
        registration = pendingRegistrationRepository.save(registration);

        NotificationSettings notificationSettings = loadNotificationSettings();
        AuthResult<VerificationStatusResponse> result = buildVerificationResult(registration, notificationSettings);
        log.info("[1962] OTP channel verified registrationId={} channel={} completed={}",
                registration.getId(), request.channel(), result.body().completed());
        return result;
    }

    @Transactional
    @Override
    public void resendOtp(ResendOtpRequest request) {
        log.info("[1963] OTP resend attempt registrationId={} channel={}", request.registrationId(), request.channel());
        PendingRegistration registration = pendingRegistrationRepository.findById(request.registrationId())
                .orElseThrow(() -> {
                    log.error("[1964] OTP resend failed, pending registration not found registrationId={}", request.registrationId());
                    return new NotFoundException("Registration not found or already completed: " + request.registrationId());
                });
        if (request.channel() == OtpChannel.EMAIL && registration.isEmailVerified()) {
            log.error("[1965] OTP resend rejected, email already verified registrationId={}", registration.getId());
            throw new ConflictException("Email is already verified");
        }
        if (request.channel() == OtpChannel.MOBILE) {
            if (registration.isMobileVerified()) {
                log.error("[1966] OTP resend rejected, mobile already verified registrationId={}", registration.getId());
                throw new ConflictException("Mobile number is already verified");
            }
            if (!registration.hasMobile()) {
                log.error("[1967] OTP resend rejected, no mobile number on file registrationId={}", registration.getId());
                throw new ConflictException("No mobile number on file");
            }
        }
        otpService.generateAndSend(registration, request.channel());
        log.info("[1968] OTP resent registrationId={} channel={}", registration.getId(), request.channel());
    }

    /**
     * Not-yet-verified -> a pending status (no tokens, registrationId set); fully verified ->
     * promotes the pending registration into a real {@link User} + CustomerProfile and issues
     * tokens in the same call. Shared by register() and verifyOtp() so both end up at the exact
     * same "am I done yet" decision.
     */
    private AuthResult<VerificationStatusResponse> buildVerificationResult(
            PendingRegistration registration, NotificationSettings notificationSettings) {
        boolean emailRequired = notificationSettings.isEmailVerificationEnabled() && !registration.isEmailVerified();
        boolean mobileRequired =
                notificationSettings.isMobileVerificationEnabled() && registration.hasMobile() && !registration.isMobileVerified();
        if (!emailRequired && !mobileRequired) {
            User user = promoteToUser(registration);
            AuthResult<AuthResponse> tokens = issueTokens(user);
            VerificationStatusResponse body = new VerificationStatusResponse(true, null, false, false, tokens.body());
            return new AuthResult<>(body, tokens.rawRefreshToken());
        }
        VerificationStatusResponse body =
                new VerificationStatusResponse(false, registration.getId(), emailRequired, mobileRequired, null);
        return new AuthResult<>(body, null);
    }

    /** No verification required at all (both channels off, or off+no mobile given) - the pre-OTP-feature behavior. */
    private User createVerifiedUser(RegisterRequest request) {
        Role customerRole = findCustomerRole();
        User user = new User();
        user.setFullName(request.fullName());
        user.setEmail(request.email());
        user.setMobileNumber(request.mobileNumber());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setEnabled(true);
        user.getRoles().add(customerRole);
        user = userRepository.save(user);
        log.info("[1003] New user created id={} email={}", user.getId(), user.getEmail());
        customerProfileRepository.save(new CustomerProfile(user));
        log.info("[1004] Customer profile created for userId={}", user.getId());
        return user;
    }

    /** Every required OTP channel is confirmed - create the real account from the stored signup data and discard the pending row. */
    private User promoteToUser(PendingRegistration registration) {
        Role customerRole = findCustomerRole();
        User user = new User();
        user.setFullName(registration.getFullName());
        user.setEmail(registration.getEmail());
        user.setMobileNumber(registration.getMobileNumber());
        user.setPasswordHash(registration.getPasswordHash());
        user.setEnabled(true);
        user.getRoles().add(customerRole);
        user = userRepository.save(user);
        log.info("[1003] New user created from verified pending registration id={} userId={} email={}",
                registration.getId(), user.getId(), user.getEmail());
        customerProfileRepository.save(new CustomerProfile(user));
        log.info("[1004] Customer profile created for userId={}", user.getId());
        pendingRegistrationRepository.delete(registration);
        return user;
    }

    private Role findCustomerRole() {
        return roleRepository.findByName(RoleName.CUSTOMER)
                .orElseThrow(() -> {
                    log.error("[1002] Registration failed, CUSTOMER role not seeded");
                    return new IllegalStateException("CUSTOMER role is not seeded");
                });
    }

    @Transactional
    @Override
    public AuthResult<AuthResponse> login(LoginRequest request) {
        log.info("[1006] Login attempt for email={}", request.email());
        User user = authenticate(request);
        AuthResult<AuthResponse> result = issueTokens(user);
        log.info("[1007] Login succeeded userId={}", user.getId());
        return result;
    }

    @Transactional
    @Override
    public AuthResult<AuthResponse> adminLogin(LoginRequest request) {
        log.info("[1008] Admin login attempt for email={}", request.email());
        User user = authenticate(request);
        if (!user.hasAnyRole(RoleName.ADMIN, RoleName.SUPER_ADMIN, RoleName.EMPLOYEE)) {
            log.error("[1009] Admin login denied, insufficient role userId={} email={}", user.getId(), user.getEmail());
            throw new AccessDeniedException("This account does not have admin access");
        }
        AuthResult<AuthResponse> result = issueTokens(user);
        log.info("[1010] Admin login succeeded userId={}", user.getId());
        return result;
    }

    @Transactional
    @Override
    public AuthResult<TokenResponse> refresh(String rawRefreshToken) {
        log.info("[1011] Refresh token attempt");
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            log.error("[1012] Refresh failed, missing refresh token");
            throw new BadCredentialsException("Missing refresh token");
        }
        Claims claims;
        try {
            claims = jwtService.parseClaims(rawRefreshToken);
        } catch (JwtException | IllegalArgumentException ex) {
            log.error("[1013] Refresh failed, could not parse token: {}", ex.getMessage());
            throw new BadCredentialsException("Invalid refresh token");
        }
        if (!jwtService.isRefreshToken(claims)) {
            log.error("[1014] Refresh failed, token is not a refresh token");
            throw new BadCredentialsException("Invalid refresh token");
        }

        String hash = jwtService.hashToken(rawRefreshToken);
        RefreshToken stored = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> {
                    log.error("[1015] Refresh failed, no stored token for presented hash");
                    return new BadCredentialsException("Invalid refresh token");
                });
        if (!stored.isActive()) {
            log.error("[1016] Refresh failed, stored token inactive/expired tokenId={}", stored.getId());
            throw new BadCredentialsException("Refresh token is expired or revoked");
        }

        stored.setRevoked(true);
        refreshTokenRepository.save(stored);
        log.info("[1017] Old refresh token revoked tokenId={} userId={}", stored.getId(), stored.getUser().getId());

        User user = stored.getUser();
        String newAccessToken = jwtService.generateAccessToken(user.getId(), user.getEmail(), user.roleNames());
        String newRefreshToken = jwtService.generateRefreshToken(user.getId());
        persistRefreshToken(user, newRefreshToken);

        log.info("[1018] New tokens issued userId={}", user.getId());
        return new AuthResult<>(TokenResponse.of(newAccessToken, jwtService.getAccessTokenTtlSeconds()), newRefreshToken);
    }

    @Transactional
    @Override
    public void logout(String rawRefreshToken) {
        log.info("[1019] Logout attempt");
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            log.info("[1020] Logout no-op, missing refresh token");
            return;
        }
        String hash = jwtService.hashToken(rawRefreshToken);
        refreshTokenRepository.findByTokenHash(hash).ifPresent(token -> {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
            log.info("[1021] Refresh token revoked on logout tokenId={} userId={}", token.getId(), token.getUser().getId());
        });
    }

    @Transactional
    @Override
    public UserSummary createAdmin(CreateAdminRequest request) {
        log.info("[1022] Create-admin attempt email={} requestedRole={}", request.email(), request.role());
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            log.error("[1023] Create-admin failed, email already exists: email={}", request.email());
            throw new ConflictException("An account with this email already exists");
        }
        // SUPER_ADMIN is never grantable through this endpoint - it's the software owner, exclusively
        // provisioned via AdminBootstrapRunner. A caller here is either the SUPER_ADMIN creating the
        // store's first ADMIN, or an existing ADMIN creating further ADMIN/EMPLOYEE accounts - see
        // AdminUserController's @PreAuthorize.
        RoleName parsedRole;
        try {
            parsedRole = RoleName.valueOf(request.role());
        } catch (IllegalArgumentException ex) {
            log.info("[1024] Create-admin role '{}' invalid, defaulting to ADMIN", request.role());
            parsedRole = RoleName.ADMIN;
        }
        final RoleName requestedRole = (parsedRole == RoleName.ADMIN || parsedRole == RoleName.EMPLOYEE)
                ? parsedRole
                : RoleName.ADMIN;
        Role role = roleRepository.findByName(requestedRole)
                .orElseThrow(() -> {
                    log.error("[1025] Create-admin failed, role not seeded: role={}", requestedRole);
                    return new IllegalStateException(requestedRole + " role is not seeded");
                });

        User user = new User();
        user.setFullName(request.fullName());
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setEnabled(true);
        user.getRoles().add(role);
        user = userRepository.save(user);

        // A new ADMIN starts with full access to every operational module (editable afterward by
        // any other ADMIN) - a new EMPLOYEE starts with none, until an ADMIN explicitly grants some.
        if (requestedRole == RoleName.ADMIN) {
            modulePermissionService.grantAllModules(user.getId());
        }

        log.info("[1026] Admin account created userId={} role={}", user.getId(), requestedRole);
        return toSummary(user);
    }

    @Transactional(readOnly = true)
    @Override
    public List<AdminStaffRow> listStaff() {
        log.info("[1935] Listing staff accounts");
        return userRepository.findByRoles_NameIn(List.of(RoleName.ADMIN, RoleName.EMPLOYEE)).stream()
                .map(u -> new AdminStaffRow(
                        u.getId(),
                        u.getFullName(),
                        u.getEmail(),
                        u.hasAnyRole(RoleName.ADMIN) ? RoleName.ADMIN.name() : RoleName.EMPLOYEE.name(),
                        u.isEnabled()))
                .toList();
    }

    @Transactional
    @Override
    public void setStaffStatus(Long userId, boolean enabled) {
        log.info("[1936] Set staff status userId={}, enabled={}", userId, enabled);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("[1937] Set staff status failed, no user for userId={}", userId);
                    return new NotFoundException("User not found: " + userId);
                });
        if (!user.hasAnyRole(RoleName.ADMIN, RoleName.EMPLOYEE)) {
            log.error("[1938] Set staff status rejected, not a staff account userId={}", userId);
            throw new ConflictException("This account is not an ADMIN or EMPLOYEE account");
        }
        // The store can never be left with zero enabled ADMIN accounts - disabling the last one
        // would permanently lock everyone (including whoever's trying to disable it) out of every
        // ADMIN-only screen, with no way back in short of a database fix.
        if (!enabled && user.hasAnyRole(RoleName.ADMIN) && userRepository.countByRoles_NameAndEnabledTrue(RoleName.ADMIN) <= 1) {
            log.error("[1939] Set staff status rejected, would leave zero enabled ADMIN accounts userId={}", userId);
            throw new ConflictException("Cannot disable the last remaining ADMIN account");
        }
        user.setEnabled(enabled);
        userRepository.save(user);
        log.info("[1940] Staff status updated userId={}, enabled={}", userId, enabled);
    }

    private User authenticate(LoginRequest request) {
        User user = userRepository.findByEmailIgnoreCase(request.email())
                .orElseThrow(() -> {
                    log.error("[1027] Authentication failed, no user for email={}", request.email());
                    return new BadCredentialsException("Invalid email or password");
                });
        if (!user.isEnabled() || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            log.error("[1028] Authentication failed, invalid credentials userId={}", user.getId());
            throw new BadCredentialsException("Invalid email or password");
        }
        log.info("[1029] Authentication succeeded userId={}", user.getId());
        return user;
    }

    private AuthResult<AuthResponse> issueTokens(User user) {
        String accessToken = jwtService.generateAccessToken(user.getId(), user.getEmail(), user.roleNames());
        String refreshToken = jwtService.generateRefreshToken(user.getId());
        persistRefreshToken(user, refreshToken);
        log.info("[1030] Tokens generated userId={} accessTtlSeconds={} refreshTtlSeconds={}",
                user.getId(), jwtService.getAccessTokenTtlSeconds(), jwtService.getRefreshTokenTtlSeconds());

        AuthResponse response = AuthResponse.of(accessToken, jwtService.getAccessTokenTtlSeconds(), toSummary(user));
        return new AuthResult<>(response, refreshToken);
    }

    private void persistRefreshToken(User user, String rawRefreshToken) {
        RefreshToken entity = new RefreshToken();
        entity.setUser(user);
        entity.setTokenHash(jwtService.hashToken(rawRefreshToken));
        entity.setExpiresAt(Instant.now().plusSeconds(jwtService.getRefreshTokenTtlSeconds()));
        entity.setRevoked(false);
        refreshTokenRepository.save(entity);
        log.info("[1031] Refresh token persisted userId={} expiresAt={}", user.getId(), entity.getExpiresAt());
    }

    @Transactional(readOnly = true)
    @Override
    public UserSummary getCurrentUser(Long userId) {
        log.info("[1032] Fetch current user userId={}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("[1033] Current user lookup failed, no user for userId={}", userId);
                    return new BadCredentialsException("Invalid token");
                });
        log.info("[1034] Current user resolved userId={}", userId);
        return toSummary(user);
    }

    @Transactional
    @Override
    public UserSummary updateProfile(Long userId, UpdateProfileRequest request) {
        log.info("[1941] Update profile attempt userId={}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("[1942] Update profile failed, no user for userId={}", userId);
                    return new NotFoundException("User not found: " + userId);
                });
        if (!user.getEmail().equalsIgnoreCase(request.email()) && userRepository.existsByEmailIgnoreCase(request.email())) {
            log.error("[1943] Update profile failed, email already in use: email={}", request.email());
            throw new ConflictException("An account with this email already exists");
        }
        if (request.newPassword() != null && !request.newPassword().isBlank()) {
            if (request.currentPassword() == null || !passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
                log.error("[1944] Update profile failed, current password mismatch userId={}", userId);
                throw new BadCredentialsException("Current password is incorrect");
            }
            user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
            log.info("[1945] Password changed userId={}", userId);
        }
        user.setFullName(request.fullName());
        user.setEmail(request.email());
        user = userRepository.save(user);
        log.info("[1946] Profile updated userId={}", userId);
        return toSummary(user);
    }

    private UserSummary toSummary(User user) {
        List<String> roles = user.roleNames().stream().map(Enum::name).sorted().toList();
        return new UserSummary(user.getId(), user.getFullName(), user.getEmail(), roles);
    }
}
