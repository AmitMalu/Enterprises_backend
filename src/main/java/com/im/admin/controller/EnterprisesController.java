package com.im.admin.controller;

import com.im.admin.entities.Enterprises;
import com.im.admin.service.EnterprisesService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

@RestController
@RequestMapping("/api/enterprises")
@Validated
@Slf4j
public class EnterprisesController {

    @Autowired
    private EnterprisesService enterprisesService;

    @PostMapping("/save")
    public ResponseEntity<?> saveEnterprise(@Valid @RequestBody Enterprises enterprise) {
        Enterprises savedEnterprise = enterprisesService.saveEnterprise(enterprise);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedEnterprise);
    }

    @PutMapping("/update/{enterpriseId}")
    public ResponseEntity<?> updateEnterprise(@PathVariable String enterpriseId, @Valid @RequestBody Enterprises enterprise) {
        Enterprises updatedEnterprise = enterprisesService.updateEnterprise(enterpriseId, enterprise);
        return ResponseEntity.ok(updatedEnterprise);
    }

    @GetMapping("/getAll")
    public ResponseEntity<List<Enterprises>> getAllEnterprises() {
        List<Enterprises> enterprises = enterprisesService.getAllEnterprises();
        return ResponseEntity.ok(enterprises);
    }

    @GetMapping("/getById/{enterpriseId}")
    public ResponseEntity<?> getEnterpriseById(@PathVariable String enterpriseId) {
        Enterprises enterprise = enterprisesService.getEnterpriseById(enterpriseId);
        return ResponseEntity.ok(enterprise);
    }

    @DeleteMapping("/delete/{enterpriseId}")
    public ResponseEntity<?> deleteEnterprise(@PathVariable String enterpriseId) {
        enterprisesService.deleteEnterprise(enterpriseId);
        return ResponseEntity.ok("Enterprise deleted successfully.");
    }

    // Handle validation errors
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));
        return ResponseEntity.badRequest().body(errors);
    }
}
