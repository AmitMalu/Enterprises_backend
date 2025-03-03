package com.im.admin.controller;

import com.im.admin.entities.SubProducts;
import com.im.admin.service.SubProductsService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@Slf4j
@RestController
@Validated
@RequestMapping("/api/sub-products")
public class SubProductsController {

    @Autowired
    private SubProductsService subProductsService;

    @PostMapping
    public ResponseEntity<SubProducts> createSubProduct(@RequestBody @Valid SubProducts subProduct) {
        log.info("Creating SubProduct: {}", subProduct);
        subProduct.setCreatedOn(LocalDateTime.now());
        return ResponseEntity.ok(subProductsService.save(subProduct));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SubProducts> updateSubProduct(@PathVariable Long id, @RequestBody @Valid SubProducts subProduct) {
        log.info("Updating SubProduct with ID: {}", id);
        return ResponseEntity.ok(subProductsService.update(id, subProduct));
    }

    @GetMapping
    public ResponseEntity<Page<SubProducts>> getAllSubProducts(@RequestParam(defaultValue = "0") int page,
                                                               @RequestParam(defaultValue = "10") int size) {
        log.info("Fetching all SubProducts with pagination - Page: {}, Size: {}", page, size);
        return ResponseEntity.ok(subProductsService.getAll(PageRequest.of(page, size)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SubProducts> getSubProductById(@PathVariable Long id) {
        log.info("Fetching SubProduct with ID: {}", id);
        return subProductsService.getById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSubProduct(@PathVariable Long id) {
        log.info("Deleting SubProduct with ID: {}", id);
        subProductsService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
