package com.im.admin.controller;

import com.im.admin.dto.RetailerDTO;
import com.im.admin.service.RetailerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/retailers")
public class RetailerController {

    private final RetailerService retailerService;

    public RetailerController(RetailerService retailerService) {
        this.retailerService = retailerService;
    }

    @PostMapping
    public ResponseEntity<RetailerDTO> saveRetailer(@RequestBody RetailerDTO retailerDTO) {
        return ResponseEntity.ok(retailerService.saveRetailer(retailerDTO));
    }

    @PutMapping("/{id}")
    public ResponseEntity<RetailerDTO> updateRetailer(@PathVariable Long id, @RequestBody RetailerDTO retailerDTO) {
        return ResponseEntity.ok(retailerService.updateRetailer(id, retailerDTO));
    }

    @GetMapping
    public ResponseEntity<List<RetailerDTO>> getAllRetailers() {
        return ResponseEntity.ok(retailerService.getAllRetailers());
    }

    @GetMapping("/{id}")
    public ResponseEntity<RetailerDTO> getRetailerById(@PathVariable Long id) {
        return ResponseEntity.ok(retailerService.getRetailerById(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRetailer(@PathVariable Long id) {
        retailerService.deleteRetailer(id);
        return ResponseEntity.noContent().build();
    }
}

