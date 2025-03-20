package com.im.admin.controller;

import com.im.admin.entities.Partner;
import com.im.admin.service.PartnerService;
import com.im.admin.service.impl.PhoneNumberValidationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api/partners")
@Slf4j
public class PartnerController {

    @Autowired
    private PartnerService partnerService;

    @Autowired
    private PhoneNumberValidationService phoneNumberValidationService;

    @PostMapping
    public ResponseEntity<Partner> createPartner(@RequestBody @Valid Partner partner) {
        log.info("Received request to create Partner: {}", partner.getName());
        phoneNumberValidationService.validatePhoneNumber(partner.getMobile());
        return ResponseEntity.ok(partnerService.savePartner(partner));
    }

    @PutMapping("/{partnerId}")
    public ResponseEntity<Partner> updatePartner(@PathVariable String partnerId, @RequestBody @Valid Partner updatedPartner) {
        log.info("Received request to update Partner with ID: {}", partnerId);
        return ResponseEntity.ok(partnerService.updatePartner(partnerId, updatedPartner));
    }

    @GetMapping
    public ResponseEntity<List<Partner>> getAllPartners() {
        log.info("Received request to fetch all Partners");
        return ResponseEntity.ok(partnerService.getAllPartners());
    }

    @GetMapping("/{partnerId}")
    public ResponseEntity<Partner> getPartnerById(@PathVariable String partnerId) {
        log.info("Received request to fetch Partner with ID: {}", partnerId);
        return ResponseEntity.ok(partnerService.getPartnerById(partnerId));
    }

    @DeleteMapping("/{partnerId}")
    public ResponseEntity<String> deletePartner(@PathVariable String partnerId) {
        log.info("Received request to delete Partner with ID: {}", partnerId);
        partnerService.deletePartner(partnerId);
        return ResponseEntity.ok("SuperDistributor deleted successfully.");    }
}
