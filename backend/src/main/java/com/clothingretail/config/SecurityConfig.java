package com.clothingretail.config;

import com.clothingretail.auth.service.JwtAuthenticationFilter;
import com.clothingretail.auth.service.JwtService;
import com.clothingretail.common.HttpErrorResponses;
import com.clothingretail.subscription.repository.SubscriptionBillingRepository;
import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtService jwtService;
    private final String allowedOrigin;
    private final SubscriptionBillingRepository subscriptionBillingRepository;

    public SecurityConfig(
            JwtService jwtService,
            @Value("${app.cors.allowed-origin}") String allowedOrigin,
            SubscriptionBillingRepository subscriptionBillingRepository) {
        this.jwtService = jwtService;
        this.allowedOrigin = allowedOrigin;
        this.subscriptionBillingRepository = subscriptionBillingRepository;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * SUPER_ADMIN (the software owner) has every privilege ADMIN (the store owner) has, on top of
     * its own software-owner-only powers (creating the first ADMIN, subscription billing) - rather
     * than repeating "hasRole('ADMIN') or hasRole('SUPER_ADMIN')" across every one of the ~25
     * @PreAuthorize-protected controllers, this single role hierarchy makes every hasRole('ADMIN')
     * check automatically pass for a SUPER_ADMIN too. Picked up automatically by @EnableMethodSecurity's
     * expression handler - no other wiring needed. EMPLOYEE deliberately does NOT inherit from ADMIN
     * (the whole point of that role is narrower, per-module access - see ModulePermission).
     */
    @Bean
    public RoleHierarchy roleHierarchy() {
        return RoleHierarchyImpl.fromHierarchy("ROLE_SUPER_ADMIN > ROLE_ADMIN");
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(
                                "/api/auth/register",
                                "/api/auth/login",
                                "/api/auth/refresh",
                                "/api/auth/logout",
                                "/api/auth/otp/verify",
                                "/api/auth/otp/resend",
                                "/api/admin/auth/login")
                        .permitAll()
                        // A real payment gateway calling this has no customer JWT - its security
                        // boundary is the HMAC signature checked inside PaymentWebhookService,
                        // not Spring Security. See PaymentController.webhook / MockPaymentGateway.
                        .requestMatchers(HttpMethod.POST, "/api/payments/webhook")
                        .permitAll()
                        // Same reasoning as the mock webhook above, for Razorpay's own delivery -
                        // see RazorpayWebhookController and RazorpayPaymentGateway.verifySignature.
                        .requestMatchers(HttpMethod.POST, "/api/payments/razorpay/webhook")
                        .permitAll()
                        .requestMatchers(HttpMethod.GET,
                                "/api/products/**",
                                "/api/categories",
                                "/api/categories/**",
                                "/api/sub-categories",
                                "/api/sub-categories/**",
                                "/api/sizes",
                                "/api/colors",
                                "/api/vendors",
                                "/api/damage-reasons",
                                "/api/configuration",
                                "/api/payments/config",
                                "/media/**")
                        .permitAll()
                        .requestMatchers(
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs/**")
                        .permitAll()
                        .anyRequest().authenticated())
                // Rejections thrown by the security filter chain itself (missing/invalid token,
                // insufficient role) happen before the DispatcherServlet ever reaches a
                // controller, so GlobalExceptionHandler never sees them - these two handlers are
                // what keep such responses in the same ApiError JSON shape as everything else.
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) ->
                                HttpErrorResponses.write(response, 401, "Unauthorized", "Authentication is required", request.getRequestURI()))
                        .accessDeniedHandler((request, response, accessDeniedException) ->
                                HttpErrorResponses.write(response, 403, "Forbidden", "You do not have permission to perform this action", request.getRequestURI())))
                .addFilterBefore(new JwtAuthenticationFilter(jwtService), UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(new SubscriptionAccessFilter(subscriptionBillingRepository), JwtAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // app.cors.allowed-origin may be a comma-separated list (e.g. localhost dev origin
        // plus a LAN IP for testing from a phone on the same network). Origin *patterns*
        // (not just exact origins) so the default list can include a wildcard for ngrok's
        // random-subdomain demo URLs (https://*.ngrok-free.app etc.) without needing to be
        // reconfigured every time a new tunnel is started - setAllowedOriginPatterns still
        // works correctly with allowCredentials(true), unlike a literal "*" origin would.
        configuration.setAllowedOriginPatterns(Arrays.stream(allowedOrigin.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList());
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
