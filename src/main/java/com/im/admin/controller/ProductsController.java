package com.im.admin.controller;

import com.im.admin.dto.ProductDropdownResponseDto;
import com.im.admin.entities.product.Products;
import com.im.admin.response.CommonResponse;
import com.im.admin.service.ProductsService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@Slf4j
@Validated
public class ProductsController {

    @Autowired
    private ProductsService productsService;

    @PostMapping
    public ResponseEntity<CommonResponse<Products>> saveProduct(@Valid @RequestBody Products product) {
        log.info("Received request to save product: {}", product);
        Products savedProduct = productsService.saveProduct(product);

        CommonResponse<Products> response = new CommonResponse<>();
        response.setStatusCode(HttpStatus.CREATED.value());
        response.setMessage("Product saved successfully");
        response.setData(savedProduct);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CommonResponse<Products>> updateProduct(@PathVariable Short id, @Valid @RequestBody Products product) {
        log.info("Received request to update product with ID: {}", id);
        Products updatedProduct = productsService.updateProduct(id, product);

        CommonResponse<Products> response = new CommonResponse<>();
        response.setStatusCode(HttpStatus.OK.value());
        response.setMessage("Product updated successfully");
        response.setData(updatedProduct);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Short id) {
        log.info("Received request to delete product with ID: {}", id);
        productsService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<Products>> getAllProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("Received request to fetch all products with pagination - Page: {}, Size: {}", page, size);
        Pageable pageable = PageRequest.of(page, size);
        List<Products> products = productsService.getAllProducts(pageable);
        return ResponseEntity.ok(products);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Products> getProductById(@PathVariable Short id) {
        log.info("Received request to fetch product with ID: {}", id);
        Products product = productsService.getProductById(id);
        return ResponseEntity.ok(product);
    }

    @GetMapping("/dropdown/active")
    public ResponseEntity<List<ProductDropdownResponseDto>> getAllActiveProducts() {
        List<ProductDropdownResponseDto> products = productsService.getAllActiveProducts();
        return ResponseEntity.ok(products);
    }
}

