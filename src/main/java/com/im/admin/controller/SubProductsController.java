package com.im.admin.controller;

import com.im.admin.dto.SubProductDropdownResponseDto;
import com.im.admin.dto.SubProductsResponseDTO;
import com.im.admin.entities.SubProducts;
import com.im.admin.response.CommonResponse;
import com.im.admin.service.SubProductsService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
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
    public ResponseEntity<CommonResponse<Page<SubProductsResponseDTO>>> getAllSubProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,

            @RequestParam(required = false) List<String> id,
            @RequestParam(required = false) String subProductTitle,
            @RequestParam(required = false) String subProductCode,
            @RequestParam(required = false) String productCode,
            @RequestParam(required = false) Short productId,
            @RequestParam(required = false) String productName,
            @RequestParam(required = false) String walletType,
            @RequestParam(required = false) Boolean isSettlement,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) Boolean enabled,
            @RequestParam(required = false) String createdBy,
            @RequestParam(required = false) String editedBy,
            @RequestParam(required = false) @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate createdOn,
            @RequestParam(required = false) @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate editedOn
    ) {

        Pageable pageable = PageRequest.of(page, size);
        Page<SubProductsResponseDTO> result = subProductsService.getAllSubProducts(
                pageable, id, subProductTitle, subProductCode, productCode,
                productId, productName, walletType, isSettlement, isActive, enabled,
                createdBy, editedBy, createdOn, editedOn
        );

        CommonResponse<Page<SubProductsResponseDTO>> response = new CommonResponse<>(200, "SubProducts fetched successfully", result);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CommonResponse<SubProductsResponseDTO>> getSubProductById(@PathVariable String id) {
        SubProductsResponseDTO result = subProductsService.getSubProductById(id);
        return ResponseEntity.ok(CommonResponse.success(result, "SubProduct fetched successfully"));
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

    @GetMapping("/dropdown")
    public ResponseEntity<CommonResponse<List<SubProductDropdownResponseDto>>> getSubProductDropdown() {
        List<SubProductDropdownResponseDto> dropdownList = subProductsService.getSubProductDropdown();
        return ResponseEntity.ok(CommonResponse.success(dropdownList, "SubProduct dropdown loaded successfully"));
    }

}

