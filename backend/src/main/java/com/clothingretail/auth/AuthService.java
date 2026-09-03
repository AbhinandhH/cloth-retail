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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
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
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new ConflictException("An account with this email already exists");
        }
        Role customerRole = roleRepository.findByName(RoleName.CUSTOMER)
                .orElseThrow(() -> new IllegalStateException("CUSTOMER role is not seeded"));

        User user = new User();
        user.setFullName(request.fullName());
        user.setEmail(request.email());
        user.setMobileNumber(request.mobileNumber());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setEnabled(true);
        user.getRoles().add(customerRole);
        user = userRepository.save(user);

        customerProfileRepository.save(new CustomerProfile(user));

        return issueTokens(user);
    }

    @Transactional
    public AuthResult<AuthResponse> login(LoginRequest request) {
        User user = authenticate(request);
        return issueTokens(user);
    }

    @Transactional
    public AuthResult<AuthResponse> adminLogin(LoginRequest request) {
        User user = authenticate(request);
        if (!user.hasAnyRole(RoleName.ADMIN, RoleName.SUPER_ADMIN)) {
            throw new AccessDeniedException("This account does not have admin access");
        }
        return issueTokens(user);
    }

    @Transactional
    public AuthResult<TokenResponse> refresh(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw new BadCredentialsException("Missing refresh token");
        }
        Claims claims;
        try {
            claims = jwtService.parseClaims(rawRefreshToken);
        } catch (JwtException | IllegalArgumentException ex) {
            throw new BadCredentialsException("Invalid refresh token");
        }
        if (!jwtService.isRefreshToken(claims)) {
            throw new BadCredentialsException("Invalid refresh token");
        }

        String hash = jwtService.hashToken(rawRefreshToken);
        RefreshToken stored = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));
        if (!stored.isActive()) {
            throw new BadCredentialsException("Refresh token is expired or revoked");
        }

        stored.setRevoked(true);
        refreshTokenRepository.save(stored);

        User user = stored.getUser();
        String newAccessToken = jwtService.generateAccessToken(user.getId(), user.getEmail(), user.roleNames());
        String newRefreshToken = jwtService.generateRefreshToken(user.getId());
        persistRefreshToken(user, newRefreshToken);

        return new AuthResult<>(TokenResponse.of(newAccessToken, jwtService.getAccessTokenTtlSeconds()), newRefreshToken);
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            return;
        }
        String hash = jwtService.hashToken(rawRefreshToken);
        refreshTokenRepository.findByTokenHash(hash).ifPresent(token -> {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
        });
    }

    @Transactional
    public UserSummary createAdmin(CreateAdminRequest request) {
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new ConflictException("An account with this email already exists");
        }
        RoleName parsedRole;
        try {
            parsedRole = RoleName.valueOf(request.role());
        } catch (IllegalArgumentException ex) {
            parsedRole = RoleName.ADMIN;
        }
        final RoleName requestedRole = (parsedRole == RoleName.ADMIN || parsedRole == RoleName.SUPER_ADMIN)
                ? parsedRole
                : RoleName.ADMIN;
        Role role = roleRepository.findByName(requestedRole)
                .orElseThrow(() -> new IllegalStateException(requestedRole + " role is not seeded"));

        User user = new User();
        user.setFullName(request.fullName());
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setEnabled(true);
        user.getRoles().add(role);
        user = userRepository.save(user);

        return toSummary(user);
    }

    private User authenticate(LoginRequest request) {
        User user = userRepository.findByEmailIgnoreCase(request.email())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));
        if (!user.isEnabled() || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid email or password");
        }
        return user;
    }

    private AuthResult<AuthResponse> issueTokens(User user) {
        String accessToken = jwtService.generateAccessToken(user.getId(), user.getEmail(), user.roleNames());
        String refreshToken = jwtService.generateRefreshToken(user.getId());
        persistRefreshToken(user, refreshToken);

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
    }

    @Transactional(readOnly = true)
    public UserSummary getCurrentUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadCredentialsException("Invalid token"));
        return toSummary(user);
    }

    private UserSummary toSummary(User user) {
        List<String> roles = user.roleNames().stream().map(Enum::name).sorted().toList();
        return new UserSummary(user.getId(), user.getFullName(), user.getEmail(), roles);
    }
}
