package com.im.admin.controller;

import com.im.admin.entities.SubProducts;
import com.im.admin.service.SubProductsService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/sub-products")
public class SubProductsController {

    @Autowired
    private SubProductsService subProductsService;

    @PostMapping
    public ResponseEntity<SubProducts> saveSubProduct(@RequestBody @Valid SubProducts subProduct) {
        SubProducts savedProduct = subProductsService.saveSubProduct(subProduct);
        return new ResponseEntity<>(savedProduct, HttpStatus.CREATED);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteSubProduct(@PathVariable String id) {
        subProductsService.deleteSubProduct(id);
        return ResponseEntity.ok("SubProduct deleted successfully.");
    }

    @PutMapping("/{id}")
    public ResponseEntity<SubProducts> updateSubProduct(@PathVariable String id, @RequestBody SubProducts subProduct) {
        log.info("Received request to update sub-product with ID: {}", id);
        return ResponseEntity.ok(subProductsService.updateSubProduct(id, subProduct));
    }

    @GetMapping
    public ResponseEntity<Page<SubProducts>> getAllSubProducts(Pageable pageable) {
        log.info("Fetching all sub-products");
        return ResponseEntity.ok(subProductsService.getAllSubProducts(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SubProducts> getSubProductById(@PathVariable String id) {
        return ResponseEntity.ok(subProductsService.getSubProductById(id));
    }
}

