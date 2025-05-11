package com.interoperability.aliexpressproject.config;

import com.interoperability.aliexpressproject.security.JwtFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    /* ─────────────  PUBLIC endpoints  ───────────── */

    @Bean
    @Order(1)
    public SecurityFilterChain publicChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher(
                        "/auth/**",
                        "/api/aliproducts/upload/xsd",
                        "/api/aliproducts/upload/rng",
                        "/xmlrpc",
                        "/services",          //  ← NEW
                        "/services/**",       //  ← kept
                        "/admin/seed"
                )
                .csrf(c -> c.disable())
                .authorizeHttpRequests(a -> a.anyRequest().permitAll())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        return http.build();
    }

    /* ─────────────  JWT‑protected CRUD  ─────────── */

    @Bean
    @Order(2)
    public SecurityFilterChain jwtChain(HttpSecurity http, JwtFilter jwt) throws Exception {
        http
                .securityMatcher("/api/aliproducts", "/api/aliproducts/**")
                .csrf(c -> c.disable())
                .addFilterBefore(jwt, UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(a -> a.anyRequest().authenticated())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        return http.build();
    }

    /* ─────────────  Fallback Basic‑Auth  ────────── */

    @Bean
    @Order(3)
    public SecurityFilterChain fallback(HttpSecurity http) throws Exception {
        http
                .csrf(c -> c.disable())
                .authorizeHttpRequests(a -> a.anyRequest().authenticated())
                .httpBasic(Customizer.withDefaults());
        return http.build();
    }
}
