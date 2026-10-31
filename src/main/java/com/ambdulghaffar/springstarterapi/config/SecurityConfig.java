package com.ambdulghaffar.springstarterapi.config;

import com.ambdulghaffar.springstarterapi.config.jwt.JwtAuthenticationFilter;
import com.ambdulghaffar.springstarterapi.config.keycloak.JitProvisioningFilter;
import com.ambdulghaffar.springstarterapi.config.keycloak.KeycloakJwtAuthConverter;
import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private static final Customizer<
            AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry
            > COMMON_RULES = auth -> auth
            .dispatcherTypeMatchers(DispatcherType.ASYNC).permitAll()
            .requestMatchers(
                    "/swagger-ui/**",
                    "/v3/api-docs/**",
                    "/swagger-ui.html"
            ).permitAll()
            .requestMatchers("/error").permitAll()
            .requestMatchers("/actuator/health").permitAll()
            .requestMatchers("/api/v1/auth/**").permitAll()
            .requestMatchers(HttpMethod.GET, "/api/v1/posts/**").permitAll()
            .requestMatchers(HttpMethod.GET, "/api/v1/comments/**").permitAll()
            .requestMatchers("/api/v1/account/**").authenticated()
            .requestMatchers("/api/v1/users/me").authenticated()
            .requestMatchers("/api/v1/users/**").hasRole("ADMIN")
            .anyRequest().authenticated();

    /**
     * Session-based authentication.
     */
    @Bean
    @Profile("session")
    public SecurityFilterChain sessionFilterChain(
            HttpSecurity http,
            CorsConfigurationSource corsConfigurationSource,
            SecurityContextRepository securityContextRepository
    ) throws Exception {

        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .securityContext(sc ->
                        sc.securityContextRepository(securityContextRepository)
                )
                .authorizeHttpRequests(COMMON_RULES)
                .exceptionHandling(exception ->
                        exception.authenticationEntryPoint(
                                new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)
                        )
                )
                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.IF_REQUIRED
                        )
                );

        return http.build();
    }

    @Bean
    @Profile("jwt")
    public SecurityFilterChain jwtFilterChain(
            HttpSecurity http,
            CorsConfigurationSource corsConfigurationSource,
            JwtAuthenticationFilter jwtAuthenticationFilter
    ) throws Exception {

        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .authorizeHttpRequests(COMMON_RULES)
                .exceptionHandling(exception ->
                        exception.authenticationEntryPoint(
                                new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)
                        )
                )
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }

    @Bean
    @Profile("keycloak")
    public SecurityFilterChain keycloakFilterChain(
            HttpSecurity http,
            CorsConfigurationSource corsConfigurationSource,
            KeycloakJwtAuthConverter keycloakJwtAuthConverter,
            JitProvisioningFilter jitProvisioningFilter
    ) throws Exception {

        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .authorizeHttpRequests(COMMON_RULES)
                .oauth2ResourceServer(oauth2 ->
                        oauth2.jwt(jwt ->
                                jwt.jwtAuthenticationConverter(
                                        keycloakJwtAuthConverter
                                )
                        )
                )
                .addFilterAfter(
                        jitProvisioningFilter,
                        BearerTokenAuthenticationFilter.class
                )
                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                );

        return http.build();
    }

    // Password encoder used by the session-based authentication.
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }


    //Security context repository used by session-based authentication.
    @Bean
    @Profile("session")
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}