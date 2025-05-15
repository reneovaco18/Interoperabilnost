
package com.interoperability.aliexpressproject.controller;

import com.interoperability.aliexpressproject.service.JaxbValidationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/aliproducts")
public class JaxbValidationController {

    private final JaxbValidationService validationService;

    public JaxbValidationController(JaxbValidationService validationService) {
        this.validationService = validationService;
    }


    @PostMapping("/validate/jaxb")
    public ResponseEntity<?> validateJaxb(@RequestParam("file") MultipartFile file) {
        try {

            String xml = new String(file.getBytes(), StandardCharsets.UTF_8);


            List<String> errors = validationService.validate(xml);


            if (!errors.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("errors", errors));
            }


            return ResponseEntity.ok(Map.of("message", "XML is valid against XSD."));
        } catch (IOException e) {
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Failed to read uploaded file: " + e.getMessage()));
        }
    }
}
