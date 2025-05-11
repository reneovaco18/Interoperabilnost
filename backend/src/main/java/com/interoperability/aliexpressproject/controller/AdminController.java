package com.interoperability.aliexpressproject.controller;

import com.interoperability.aliexpressproject.tools.DataSeeder;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final DataSeeder seeder;

    /** POST /admin/seed  – returns 200 with human text or 500 on failure */
    @PostMapping("/seed")
    public ResponseEntity<String> seed() {
        try {
            int stored = seeder.seed().size();
            return ResponseEntity.ok("Seed finished. Stored " + stored + " products.");
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body("Seeder crashed: " + e.getMessage());
        }
    }

}
