package com.im.admin.controller;

import com.im.admin.dto.DistributorDTO;
import com.im.admin.service.DistributorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/distributors")
@RequiredArgsConstructor
public class DistributorController {

    @Autowired
    private DistributorService distributorService;

    @PostMapping
    public ResponseEntity<?> saveDistributor(@Valid @RequestBody DistributorDTO distributorDTO) {
        DistributorDTO savedDistributor = distributorService.saveDistributor(distributorDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedDistributor);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateDistributor(@PathVariable Long id, @Valid @RequestBody DistributorDTO distributorDTO) {
        DistributorDTO updatedDistributor = distributorService.updateDistributor(id, distributorDTO);
        return ResponseEntity.ok(updatedDistributor);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getDistributorById(@PathVariable Long id) {
        return ResponseEntity.ok(distributorService.getDistributorById(id));
    }

    @GetMapping
    public ResponseEntity<?> getAllDistributors(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(distributorService.getAllDistributors(page, size));
    }
}

