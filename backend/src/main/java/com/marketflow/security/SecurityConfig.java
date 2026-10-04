package com.marketflow.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Pre-flight CORS OPTIONS requests
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // Public Auth endpoints (signup, login, refresh, logout)
                        .requestMatchers(
                                "/auth/register", "/api/auth/register",
                                "/auth/login", "/api/auth/login",
                                "/auth/refresh", "/api/auth/refresh",
                                "/auth/logout", "/api/auth/logout"
                        ).permitAll()
                        // Protected Auth endpoints
                        .requestMatchers(
                                "/auth/me", "/api/auth/me",
                                "/auth/logout-all", "/api/auth/logout-all"
                        ).authenticated()
                        // Public Health & Keep-Alive endpoints
                        .requestMatchers("/health", "/api/health", "/ping", "/api/ping").permitAll()
                        // Public WebSocket handshake endpoints
                        .requestMatchers("/ws/**", "/api/ws/**").permitAll()
                        // Public Webhook ingestion endpoints (called by 3rd party providers)
                        .requestMatchers("/webhooks/**", "/api/webhooks/**").permitAll()
                        // Public Templates catalog (browse marketing templates)
                        .requestMatchers(HttpMethod.GET, "/templates/**", "/api/templates/**").permitAll()
                        // Public OpenAPI / Swagger Documentation
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/api-docs/**",
                                "/api/swagger-ui/**",
                                "/api/v3/api-docs/**"
                        ).permitAll()
                        // All workflow creation, execution, AI generation, and metrics require valid JWT
                        .requestMatchers("/workflows/**", "/api/workflows/**").authenticated()
                        .requestMatchers("/executions/**", "/api/executions/**").authenticated()
                        .requestMatchers("/ai/**", "/api/ai/**").authenticated()
                        .requestMatchers("/metrics", "/api/metrics").authenticated()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(jakarta.servlet.http.HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType(org.springframework.http.MediaType.APPLICATION_JSON_VALUE);
                            response.getWriter().write("{\"error\":{\"code\":\"UNAUTHORIZED\",\"message\":\"Full authentication is required to access this resource\"}}");
                        })
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
