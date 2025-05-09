// src/main/java/com/interoperability/aliexpressproject/config/CrudSecurityConfig.java
package com.interoperability.aliexpressproject.config;

import com.interoperability.aliexpressproject.security.JwtFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class CrudSecurityConfig {

    private final JwtFilter jwtFilter;

    public CrudSecurityConfig(JwtFilter jwtFilter) {
        this.jwtFilter = jwtFilter;
    }

    /**
     * Chain #2: secures all /api/aliproducts/** endpoints with JWT.
     * Must run after the “publicChain” (Order 1) but before the HTTP-Basic chain (lowest).
     */
    @Bean
    @Order(2)
    public SecurityFilterChain crudChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/api/aliproducts/**")
                .csrf(csrf -> csrf.disable())
                // insert your JwtFilter so it extracts the user from the Bearer token
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().authenticated()
                );
        return http.build();
    }
}
