// src/main/java/com/interoperability/aliexpressproject/controller/AdminController.java
package com.interoperability.aliexpressproject.controller;

import com.interoperability.aliexpressproject.tools.DataSeeder;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// 1.  tiny REST wrapper ---------------------------------------
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final DataSeeder seeder;

    @PostMapping("/seed")
    public ResponseEntity<String> seed() {
        try {
            seeder.run();                      // <‑‑ make it public
            return ResponseEntity.ok("Seeded 20 products.");
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Seeder failed: " + e.getMessage());
        }
    }
}
