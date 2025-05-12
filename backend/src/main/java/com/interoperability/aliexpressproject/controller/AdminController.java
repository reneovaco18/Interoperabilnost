
package com.interoperability.aliexpressproject.controller;

import com.interoperability.aliexpressproject.tools.DataSeeder;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final DataSeeder seeder;

    @PostMapping("/seed")
    public ResponseEntity<String> seed() {
        try {
            int tried = seeder.seed();
            return ResponseEntity.ok("Seed finished. Attempted " + tried + " products.");
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body("Seeder crashed: " + e.getMessage());
        }
    }
}
