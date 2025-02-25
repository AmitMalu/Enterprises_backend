package com.im.admin.controller;


import com.im.admin.entities.SuperDistributor;
import com.im.admin.service.SuperDistributorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/super-distributors")
@RequiredArgsConstructor
@Validated
public class SuperDistributorController {

    @Autowired
    private SuperDistributorService service;

    @PostMapping
    public ResponseEntity<SuperDistributor> save(@Valid @RequestBody SuperDistributor distributor) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.saveSuperDistributor(distributor));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SuperDistributor> get(@PathVariable Long id) {
        return ResponseEntity.ok(service.getSuperDistributor(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SuperDistributor> update(@PathVariable Long id, @Valid @RequestBody SuperDistributor distributor) {
        return ResponseEntity.ok(service.updateSuperDistributor(id, distributor));
    }

    @GetMapping
    public ResponseEntity<List<SuperDistributor>> getAll() {
        return ResponseEntity.ok(service.getAllSuperDistributors());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(@PathVariable Long id) {
        service.deleteSuperDistributor(id);
        return ResponseEntity.ok("SuperDistributor deleted successfully.");
    }
}

