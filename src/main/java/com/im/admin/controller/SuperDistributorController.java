package com.im.admin.controller;


import com.im.admin.entities.SuperDistributor;
import com.im.admin.service.SuperDistributorService;
import com.im.admin.service.impl.PhoneNumberValidationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/super-distributors")
@RequiredArgsConstructor
@Validated
public class SuperDistributorController {

    @Autowired
    private SuperDistributorService service;

    @Autowired
    private PhoneNumberValidationService phoneNumberValidationService;

    @PostMapping
    public ResponseEntity<SuperDistributor> save(@Valid @RequestBody SuperDistributor distributor) {
        phoneNumberValidationService.validatePhoneNumber(distributor.getMobile());
        //return ResponseEntity.ok(whiteLabelService.saveWhiteLabel(whiteLabel));

        return ResponseEntity.status(HttpStatus.CREATED).body(service.saveSuperDistributor(distributor));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SuperDistributor> get(@PathVariable String id) {
        return ResponseEntity.ok(service.getSuperDistributor(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SuperDistributor> update(@PathVariable String id, @Valid @RequestBody SuperDistributor distributor) {
        return ResponseEntity.ok(service.updateSuperDistributor(id, distributor));
    }

    @GetMapping
    public ResponseEntity<List<SuperDistributor>> getAll() {
        return ResponseEntity.ok(service.getAllSuperDistributors());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(@PathVariable String id) {
        service.deleteSuperDistributor(id);
        return ResponseEntity.ok("SuperDistributor deleted successfully.");
    }
}

