// src/main/java/com/interoperability/aliexpressproject/config/SecurityConfig.java
package com.interoperability.aliexpressproject.config;

import com.interoperability.aliexpressproject.security.JwtFilter;
import org.springframework.context.annotation.*;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.*;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Three chains:
 *  1. everything under /auth/**, the XML upload endpoints, SOAP (/services/**) and XML‑RPC (/xmlrpc) = PUBLIC
 *  2. /api/aliproducts … = JWT protected
 *  3. everything else falls back to HTTP Basic (mostly actuator / H2 console etc.)
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean @Order(1)
    public SecurityFilterChain publicChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher(
                        "/auth/**",
                        "/api/aliproducts/upload/xsd",
                        "/api/aliproducts/upload/rng",
                        "/xmlrpc",
                        "/services/**",
                        "/admin/seed"
                )

                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(a -> a.anyRequest().permitAll())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        return http.build();
    }

    @Bean @Order(2)
    public SecurityFilterChain jwtChain(HttpSecurity http,
                                        JwtFilter jwtFilter) throws Exception {
        http
                .securityMatcher("/api/aliproducts", "/api/aliproducts/**")
                .csrf(csrf -> csrf.disable())
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(a -> a.anyRequest().authenticated())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        return http.build();
    }

    @Bean @Order(3)
    public SecurityFilterChain fallbackChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(a -> a.anyRequest().authenticated())
                .httpBasic(Customizer.withDefaults());
        return http.build();
    }
}
