package com.im.admin.controller;

import com.im.admin.entities.Distributor;
import com.im.admin.service.DistributorService;
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
@RequestMapping("/api/distributors")
@RequiredArgsConstructor
@Validated
public class DistributorController {

    @Autowired
    private DistributorService distributorService;

    @Autowired
    private PhoneNumberValidationService phoneNumberValidationService;

    @PostMapping
    public ResponseEntity<Distributor> createDistributor(@Valid @RequestBody Distributor distributor) {
        phoneNumberValidationService.validatePhoneNumber(distributor.getMobile());
        Distributor savedDistributor = distributorService.createDistributor(distributor);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedDistributor);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Distributor> updateDistributor(
            @PathVariable String id, @Valid @RequestBody Distributor distributor) {
        Distributor updatedDistributor = distributorService.updateDistributor(id, distributor);
        return ResponseEntity.ok(updatedDistributor);
    }

    @GetMapping
    public ResponseEntity<List<Distributor>> getAllDistributors() {
        return ResponseEntity.ok(distributorService.getAllDistributors());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Distributor> getDistributorById(@PathVariable String id) {
        return ResponseEntity.ok(distributorService.getDistributorById(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteDistributor(@PathVariable String id) {
        distributorService.deleteDistributor(id);
        return ResponseEntity.ok("Distributor deleted successfully." + id);
}
}


