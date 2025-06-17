package com.im.admin.controller;

import com.im.admin.dto.ProductDropdownResponseDto;
import com.im.admin.entities.product.Products;
import com.im.admin.response.CommonResponse;
import com.im.admin.service.ProductsService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
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
    public ResponseEntity<CommonResponse<Page<Products>>> getAllProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            // Basic Filters
            @RequestParam(required = false) List<Short> id,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String productCode,
            @RequestParam(required = false) String productCodeForScheme,
            @RequestParam(required = false) String productGroup,
            @RequestParam(required = false) Boolean isShow,
            @RequestParam(required = false) Boolean hasCharges,
            @RequestParam(required = false) Boolean isCommissions,
            @RequestParam(required = false) Boolean isInventory,
            @RequestParam(required = false) Boolean isSettlement,
            @RequestParam(required = false) Boolean isVirtual,
            @RequestParam(required = false) Boolean isVisible,
            @RequestParam(required = false) Boolean reverseTx,
            @RequestParam(required = false) Boolean clusterMode,
            @RequestParam(required = false) Boolean blocked,
            @RequestParam(required = false) Boolean active
    ) {

        log.info("Received request to fetch all products with pagination - Page: {}, Size: {}", page, size);

        Pageable pageable = PageRequest.of(page, size);
        Page<Products> productsPage = productsService.getAllProducts(pageable,id,name, productCode, productCodeForScheme,
                productGroup, isShow, hasCharges, isCommissions, isInventory,
                isSettlement, isVirtual, isVisible, reverseTx, clusterMode,
                blocked, active);

        CommonResponse<Page<Products>> response = new CommonResponse<>(
                200, "Products fetched successfully", productsPage
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CommonResponse<Products>> getProductById(@PathVariable Short id) {
        log.info("Received request to fetch product with ID: {}", id);

        Products product = productsService.getProductById(id);

        CommonResponse<Products> response = new CommonResponse<>(
                200, "Product fetched successfully", product
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/dropdown/active")
    public ResponseEntity<CommonResponse<List<ProductDropdownResponseDto>>> getAllActiveProducts() {
        List<ProductDropdownResponseDto> products = productsService.getAllActiveProducts();

        CommonResponse<List<ProductDropdownResponseDto>> response = new CommonResponse<>(
                200, "Active products fetched successfully", products
        );

        return ResponseEntity.ok(response);
    }

}

