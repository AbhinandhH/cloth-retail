package com.clothingretail.auth;

import com.clothingretail.auth.dto.AuthResponse;
import com.clothingretail.auth.dto.CreateAdminRequest;
import com.clothingretail.auth.dto.LoginRequest;
import com.clothingretail.auth.dto.RegisterRequest;
import com.clothingretail.auth.dto.TokenResponse;
import com.clothingretail.auth.dto.UserSummary;
import com.clothingretail.common.ConflictException;
import com.clothingretail.customer.CustomerProfile;
import com.clothingretail.customer.CustomerProfileRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import java.time.Instant;
import java.util.List;
import lombok.extern.log4j.Log4j2;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Log4j2
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final CustomerProfileRepository customerProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            RefreshTokenRepository refreshTokenRepository,
            CustomerProfileRepository customerProfileRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.customerProfileRepository = customerProfileRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResult<AuthResponse> register(RegisterRequest request) {
        log.info("[1000] Registration attempt for email={}", request.email());
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            log.error("[1001] Registration failed, email already exists: email={}", request.email());
            throw new ConflictException("An account with this email already exists");
        }
        Role customerRole = roleRepository.findByName(RoleName.CUSTOMER)
                .orElseThrow(() -> {
                    log.error("[1002] Registration failed, CUSTOMER role not seeded");
                    return new IllegalStateException("CUSTOMER role is not seeded");
                });

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

        AuthResult<AuthResponse> result = issueTokens(user);
        log.info("[1005] Registration completed userId={}", user.getId());
        return result;
    }

    @Transactional
    public AuthResult<AuthResponse> login(LoginRequest request) {
        log.info("[1006] Login attempt for email={}", request.email());
        User user = authenticate(request);
        AuthResult<AuthResponse> result = issueTokens(user);
        log.info("[1007] Login succeeded userId={}", user.getId());
        return result;
    }

    @Transactional
    public AuthResult<AuthResponse> adminLogin(LoginRequest request) {
        log.info("[1008] Admin login attempt for email={}", request.email());
        User user = authenticate(request);
        if (!user.hasAnyRole(RoleName.ADMIN, RoleName.SUPER_ADMIN)) {
            log.error("[1009] Admin login denied, insufficient role userId={} email={}", user.getId(), user.getEmail());
            throw new AccessDeniedException("This account does not have admin access");
        }
        AuthResult<AuthResponse> result = issueTokens(user);
        log.info("[1010] Admin login succeeded userId={}", user.getId());
        return result;
    }

    @Transactional
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
    public UserSummary createAdmin(CreateAdminRequest request) {
        log.info("[1022] Create-admin attempt email={} requestedRole={}", request.email(), request.role());
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            log.error("[1023] Create-admin failed, email already exists: email={}", request.email());
            throw new ConflictException("An account with this email already exists");
        }
        RoleName parsedRole;
        try {
            parsedRole = RoleName.valueOf(request.role());
        } catch (IllegalArgumentException ex) {
            log.info("[1024] Create-admin role '{}' invalid, defaulting to ADMIN", request.role());
            parsedRole = RoleName.ADMIN;
        }
        final RoleName requestedRole = (parsedRole == RoleName.ADMIN || parsedRole == RoleName.SUPER_ADMIN)
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

        log.info("[1026] Admin account created userId={} role={}", user.getId(), requestedRole);
        return toSummary(user);
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

    private UserSummary toSummary(User user) {
        List<String> roles = user.roleNames().stream().map(Enum::name).sorted().toList();
        return new UserSummary(user.getId(), user.getFullName(), user.getEmail(), roles);
    }
}
