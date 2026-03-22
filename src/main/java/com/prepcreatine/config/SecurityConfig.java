package com.prepcreatine.config;

import com.prepcreatine.demo.DemoUserFilter;
import com.prepcreatine.security.JwtAuthenticationFilter;
import com.prepcreatine.security.PrepCreatineUserDetailsService;
import com.prepcreatine.security.RateLimitFilter;
import com.prepcreatine.security.RequestIdFilter;
import org.springframework.beans.factory.annotation.Autowired;
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
 * BSDD v2.1 §5 [MOD]: supports demo mode branch — when DEMO_MODE=true,
 * all requests are permitted and DemoUserFilter injects the demo user.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RateLimitFilter rateLimitFilter;
    private final RequestIdFilter requestIdFilter;
    private final PrepCreatineUserDetailsService userDetailsService;
    private final DemoModeConfig demoModeConfig;

    // DemoUserFilter is optional — only present when app.demo-mode=true
    @Autowired(required = false)
    private DemoUserFilter demoUserFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          RateLimitFilter rateLimitFilter,
                          RequestIdFilter requestIdFilter,
                          PrepCreatineUserDetailsService userDetailsService,
                          DemoModeConfig demoModeConfig) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.rateLimitFilter = rateLimitFilter;
        this.requestIdFilter = requestIdFilter;
        this.userDetailsService = userDetailsService;
        this.demoModeConfig = demoModeConfig;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        // ── DEMO MODE: ALL requests permitted, DemoUserFilter handles identity ──────────
        if (demoModeConfig.isDemoMode()) {
            http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(org.springframework.security.config.Customizer.withDefaults())
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .sessionManagement(session ->
                    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

            // Inject DemoUserFilter instead of JwtAuthenticationFilter
            if (demoUserFilter != null) {
                http.addFilterBefore(demoUserFilter, UsernamePasswordAuthenticationFilter.class);
            }
            return http.build();
        }

        // ── PRODUCTION MODE ──────────────────────────────────────────────────────────────
        http
            .cors(org.springframework.security.config.Customizer.withDefaults())
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.POST, "/api/auth/signup").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/logout").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/forgot-password").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/reset-password").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/verify-email").permitAll()
                .requestMatchers(HttpMethod.GET,  "/api/auth/**").permitAll()
                .requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/progress/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/waitlist").permitAll()
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                .requestMatchers("/api/mentor/**").hasRole("MENTOR")
                .requestMatchers("/api/**").authenticated()
                .anyRequest().permitAll()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(rateLimitFilter, JwtAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
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
