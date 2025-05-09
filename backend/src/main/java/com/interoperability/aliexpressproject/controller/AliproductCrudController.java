package com.interoperability.aliexpressproject.controller;

import com.interoperability.aliexpressproject.model.Aliproduct;
import com.interoperability.aliexpressproject.service.AliproductCrudService;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/aliproducts")
public class AliproductCrudController {
    private final AliproductCrudService svc;

    public AliproductCrudController(AliproductCrudService svc) {
        this.svc = svc;
    }

    @GetMapping
    public List<Aliproduct> all() throws Exception {
        return svc.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Aliproduct> one(@PathVariable String id) throws Exception {
        return svc.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Aliproduct> create(@RequestBody Aliproduct p) throws Exception {
        Aliproduct saved = svc.save(p);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Aliproduct> update(@PathVariable String id,
                                             @RequestBody Aliproduct p) throws Exception {
        return svc.update(id, p)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) throws Exception {
        return svc.delete(id)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }
}
