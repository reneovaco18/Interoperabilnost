package com.interoperability.aliexpressproject.controller;

import com.interoperability.aliexpressproject.service.AliproductService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/aliproducts")
public class AliproductController {

    private final AliproductService service;

    public AliproductController(AliproductService service) {
        this.service = service;
    }


    @PostMapping("/upload/xsd")
    public ResponseEntity<?> uploadXsd(@RequestParam("file") MultipartFile file) throws IOException {
        List<String> errors = service.validateAndSaveXsd(file);
        if (!errors.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("errors", errors));
        }
        return ResponseEntity.ok(Map.of("message", "File validated and saved."));
    }


    @PostMapping("/upload/rng")
    public ResponseEntity<?> uploadRng(@RequestParam("file") MultipartFile file) throws IOException {
        List<String> errors = service.validateAndSaveRng(file);
        if (!errors.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("errors", errors));
        }
        return ResponseEntity.ok(Map.of("message", "File validated and saved."));
    }
}
