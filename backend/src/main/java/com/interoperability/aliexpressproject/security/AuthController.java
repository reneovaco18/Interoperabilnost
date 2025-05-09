// AuthController.java
package com.interoperability.aliexpressproject.security;

import io.jsonwebtoken.JwtException;          // ← add this
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.jsonwebtoken.JwtException;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final JwtUtil jwt;

    public AuthController(JwtUtil jwt) {
        this.jwt = jwt;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestParam String username,
                                   @RequestParam String password) {
        // ← your user check
        if (!"user".equals(username) || !"pass".equals(password)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String access  = jwt.generateAccessToken(username);
        String refresh = jwt.generateRefreshToken(username);
        return ResponseEntity.ok(Map.of(
                "accessToken",  access,
                "refreshToken", refresh
        ));
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestParam String refreshToken) {
        try {
            String user = jwt.validateAndGetUsername(refreshToken);
            String access = jwt.generateAccessToken(user);
            return ResponseEntity.ok(Map.of("accessToken", access));
        } catch (JwtException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }
}
