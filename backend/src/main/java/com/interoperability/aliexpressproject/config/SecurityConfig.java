// src/main/java/com/interoperability/aliexpressproject/config/SecurityConfig.java
package com.interoperability.aliexpressproject.config;

import com.interoperability.aliexpressproject.security.JwtFilter;
import org.springframework.context.annotation.*;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.*;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    // 1) Public: permit everything under /auth/**, your upload endpoints, XML-RPC & WSDL
    @Bean
    @Order(1)
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
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }

    // 2) JWT-protected: everything under /api/aliproducts/**
    @Bean
    @Order(2)
    public SecurityFilterChain apiChain(HttpSecurity http,
                                        JwtFilter jwtFilter) throws Exception {
        http
                .securityMatcher("/api/aliproducts/**")
                .csrf(csrf -> csrf.disable())
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated());
        return http.build();
    }

    // 3) Fallback: any other request → HTTP Basic
    @Bean
    @Order(3)
    public SecurityFilterChain defaultChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                .httpBasic(Customizer.withDefaults());
        return http.build();
    }
}
