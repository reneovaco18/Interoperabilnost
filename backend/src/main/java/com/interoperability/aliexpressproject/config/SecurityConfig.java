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

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    // 1) public (no auth) endpoints
    @Bean @Order(1)
    public SecurityFilterChain publicChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher(
                        "/auth/**",
                        "/api/aliproducts/upload/xsd",
                        "/api/aliproducts/upload/rng",
                        "/xmlrpc",
                        "/services/**"
                )
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(a -> a.anyRequest().permitAll())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        return http.build();
    }

    // 2) everything under /api/aliproducts **including** the exact
    //    /api/aliproducts path (list‐all) now runs through JWTFilter
    @Bean @Order(2)
    public SecurityFilterChain jwtChain(HttpSecurity http,
                                        JwtFilter jwtFilter) throws Exception {
        http
                .securityMatcher(
                        "/api/aliproducts",      // exact list‐all
                        "/api/aliproducts/**"    // get by id, post, put, delete
                )
                .csrf(csrf -> csrf.disable())
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(a -> a.anyRequest().authenticated())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        return http.build();
    }

    // 3) fallback → HTTP Basic
    @Bean @Order(3)
    public SecurityFilterChain fallbackChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(a -> a.anyRequest().authenticated())
                .httpBasic(Customizer.withDefaults());
        return http.build();
    }
}
