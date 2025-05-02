package com.interoperability.aliexpressproject.controller;

import com.interoperability.aliexpressproject.service.AliproductService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/aliproducts")
public class AliproductController {

    private final AliproductService svc;

    public AliproductController(AliproductService svc) {
        this.svc = svc;
    }

    @PostMapping("/upload/xsd")
    public ResponseEntity<?> uploadXsd(@RequestParam("file") MultipartFile f) {
        return handleUpload(f, true);
    }

    @PostMapping("/upload/rng")
    public ResponseEntity<?> uploadRng(@RequestParam("file") MultipartFile f) {
        return handleUpload(f, false);
    }

    /* ---------- helper ---------- */

    private ResponseEntity<?> handleUpload(MultipartFile f, boolean xsd) {
        try {
            List<String> errs = xsd
                    ? svc.validateAndSaveXsd(f)
                    : svc.validateAndSaveRng(f);
            if (!errs.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("errors", errs));
            }
            String msg = "Validated against " + (xsd ? "XSD" : "RNG") + " and saved.";
            return ResponseEntity.ok(Map.of("message", msg));
        } catch (IOException ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("errors", List.of(ex.getMessage())));
        }
    }
}
