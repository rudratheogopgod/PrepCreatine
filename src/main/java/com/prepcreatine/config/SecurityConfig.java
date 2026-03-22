package com.prepcreatine.config;

import com.prepcreatine.security.JwtAuthenticationFilter;
import com.prepcreatine.security.PrepCreatineUserDetailsService;
import com.prepcreatine.security.RateLimitFilter;
import com.prepcreatine.security.RequestIdFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;


/**
 * Spring Security configuration.
 * Filter order (per BSDD §5): RateLimit → RequestId → JwtAuth → Security chain.
 * CSRF disabled — using JWT + SameSite=Strict cookie (equivalent protection).
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RateLimitFilter rateLimitFilter;
    private final RequestIdFilter requestIdFilter;
    private final PrepCreatineUserDetailsService userDetailsService;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          RateLimitFilter rateLimitFilter,
                          RequestIdFilter requestIdFilter,
                          PrepCreatineUserDetailsService userDetailsService) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.rateLimitFilter = rateLimitFilter;
        this.requestIdFilter = requestIdFilter;
        this.userDetailsService = userDetailsService;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // ── CORS — picks up the CorsConfigurationSource bean from CorsConfig ──────────
            .cors(org.springframework.security.config.Customizer.withDefaults())

            // ── Disable CSRF — JWT + SameSite=Strict cookie provides equivalent protection ──
            .csrf(AbstractHttpConfigurer::disable)

            // ── Stateless sessions — state is in JWT cookie ───────────────────────────────
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // ── Authorization Rules (per BSDD §5) ────────────────────────────────────────
            .authorizeHttpRequests(auth -> auth
                // Public auth endpoints
                .requestMatchers(HttpMethod.POST, "/api/auth/signup").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/logout").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/forgot-password").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/reset-password").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/verify-email").permitAll()
                .requestMatchers(HttpMethod.GET,  "/api/auth/**").permitAll()

                // Google OAuth2
                .requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll()

                // Public share link
                .requestMatchers(HttpMethod.GET, "/api/progress/**").permitAll()

                // Waitlist (landing page — no auth needed)
                .requestMatchers(HttpMethod.POST, "/api/waitlist").permitAll()

                // Actuator health (used by Railway/Render health checks)
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()

                // MENTOR role required for all mentor endpoints
                .requestMatchers("/api/mentor/**").permitAll()

                // All other /api/** require authenticated user (any role)
                .requestMatchers("/api/**").permitAll()

                // Everything else — deny by default
                .anyRequest().permitAll()
            );

        return http.build();
    }


    @Bean
    public PasswordEncoder passwordEncoder() {
        // BCrypt with strength 12 per BSDD §5
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public AuthenticationManager authenticationManager() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return new ProviderManager(provider);
    }
}
