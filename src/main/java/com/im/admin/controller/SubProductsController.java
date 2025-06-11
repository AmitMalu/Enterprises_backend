package com.im.admin.controller;

import com.im.admin.dto.SubProductDropdownResponseDto;
import com.im.admin.entities.SubProducts;
import com.im.admin.response.CommonResponse;
import com.im.admin.service.SubProductsService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/sub-products")
public class SubProductsController {

    @Autowired
    private SubProductsService subProductsService;

    @PostMapping
    public ResponseEntity<CommonResponse<SubProducts>> saveSubProduct(@RequestBody @Valid SubProducts subProduct) {
        try {
            SubProducts savedProduct = subProductsService.saveSubProduct(subProduct);
            CommonResponse<SubProducts> response = new CommonResponse<>(201, "SubProduct created successfully", savedProduct);
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (IllegalArgumentException ex) {
            CommonResponse<SubProducts> response = new CommonResponse<>(400, ex.getMessage(), null);
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteSubProduct(@PathVariable String id) {
        subProductsService.deleteSubProduct(id);
        return ResponseEntity.ok("SubProduct deleted successfully.");
    }

    @PutMapping("/{id}")
    public ResponseEntity<CommonResponse<SubProducts>> updateSubProduct(
            @PathVariable String id,
            @RequestBody @Valid SubProducts subProduct) {

        log.info("Received request to update sub-product with ID: {}", id);

        try {
            SubProducts updated = subProductsService.updateSubProduct(id, subProduct);
            CommonResponse<SubProducts> response = new CommonResponse<>(200, "SubProduct updated successfully", updated);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException ex) {
            log.error("Error updating sub-product: {}", ex.getMessage());
            return ResponseEntity.badRequest().body(new CommonResponse<>(400, ex.getMessage(), null));
        }
    }

    @GetMapping
    public ResponseEntity<CommonResponse<Page<SubProducts>>> getAllSubProducts(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<SubProducts> subProductsPage = subProductsService.getAllSubProducts(pageable);

        CommonResponse<Page<SubProducts>> response = new CommonResponse<>(
                200,
                "SubProducts fetched successfully",
                subProductsPage
        );

        return ResponseEntity.ok(response);
    }


    @GetMapping("/{id}")
    public ResponseEntity<SubProducts> getSubProductById(@PathVariable String id) {
        return ResponseEntity.ok(subProductsService.getSubProductById(id));
    }

    @GetMapping("/dropdown/{productId}")
    public ResponseEntity<CommonResponse<List<SubProductDropdownResponseDto>>> getActiveSubProducts(
            @PathVariable("productId") Short productId) {

        List<SubProductDropdownResponseDto> subProducts = subProductsService.getActiveSubProductsByProductId(productId);

        CommonResponse<List<SubProductDropdownResponseDto>> response = new CommonResponse<>(
                200,
                "Active sub-products fetched successfully",
                subProducts
        );

        return ResponseEntity.ok(response);
    }

}

