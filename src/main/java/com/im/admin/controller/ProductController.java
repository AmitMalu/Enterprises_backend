package com.im.admin.controller;

import com.im.admin.entities.product.Products;
import com.im.admin.service.ProductService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/products")
@Validated
@Slf4j
class ProductController {

    @Autowired
    private ProductService productService;

    @PostMapping
    public ResponseEntity<Products> createProduct(@RequestBody @Valid Products product) {
        return ResponseEntity.ok(productService.saveProduct(product));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Products> updateProduct(@PathVariable Long id, @RequestBody @Valid Products product) {
        return ResponseEntity.ok(productService.updateProduct(id, product));
    }

    @GetMapping
    public ResponseEntity<Page<Products>> getAllProducts(Pageable pageable) {
        return ResponseEntity.ok(productService.getAllProducts(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Products> getProductById(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getProductById(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }
}
