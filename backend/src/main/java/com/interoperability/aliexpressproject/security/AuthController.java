// src/main/java/com/interoperability/aliexpressproject/security/AuthController.java
package com.interoperability.aliexpressproject.security;

import io.jsonwebtoken.JwtException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final JwtUtil   jwt;
    private final UserStore store;

    public AuthController(JwtUtil jwt, UserStore store) {
        this.jwt   = jwt;
        this.store = store;
    }

    /* ─────────────────────────  REGISTRATION  ───────────────────────── */

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestParam String username,
                                      @RequestParam String password) {
        if (username.isBlank() || password.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Username and password must not be blank"));
        }
        if (!store.addUser(username, password)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "Username already exists"));
        }
        return ResponseEntity.ok(Map.of("message", "User created – now log in"));
    }

    /* ─────────────────────────────  LOGIN  ───────────────────────────── */

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestParam String username,
                                   @RequestParam String password) {

        if (!store.validCredentials(username, password)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String access  = jwt.generateAccessToken(username);
        String refresh = jwt.generateRefreshToken(username);
        return ResponseEntity.ok(Map.of(
                "accessToken",  access,
                "refreshToken", refresh
        ));
    }

    /* ───────────────────────────  REFRESH  ──────────────────────────── */

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestParam String refreshToken) {
        try {
            String user   = jwt.validateAndGetUsername(refreshToken);
            String access = jwt.generateAccessToken(user);
            return ResponseEntity.ok(Map.of("accessToken", access));
        } catch (JwtException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }
}
