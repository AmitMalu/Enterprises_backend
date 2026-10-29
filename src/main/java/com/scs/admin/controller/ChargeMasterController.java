package com.scs.admin.controller;

import com.scs.admin.dto.AutoChargeMasterRequestDTO;
import com.scs.admin.entities.ChargeMaster;
import com.scs.admin.service.ChargeMasterService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/charge-masters")
@RequiredArgsConstructor
public class ChargeMasterController {

    @Autowired
    private ChargeMasterService service;

    @PostMapping("/save")
    public ResponseEntity<ChargeMaster> create(@Valid @RequestBody ChargeMaster cm) {
        log.info("API Call: Create ChargeMaster, Request Body: {}", cm);
        return ResponseEntity.ok(service.createChargeMaster(cm));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ChargeMaster> update(@PathVariable Long id, @Valid @RequestBody ChargeMaster cm) {
        log.info("API Call: Update ChargeMaster, ID: {}, Request Body: {}", id, cm);
        return ResponseEntity.ok(service.updateChargeMaster(id, cm));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        log.info("API Call: Delete ChargeMaster, ID: {}", id);
        service.deleteChargeMaster(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ChargeMaster> getById(@PathVariable Long id) {
        log.info("API Call: Get ChargeMaster by ID, ID: {}", id);
        return ResponseEntity.ok(service.getChargeMaster(id));
    }

    @GetMapping
    public ResponseEntity<List<ChargeMaster>> getAll() {
        log.info("API Call: Get all ChargeMasters");
        return ResponseEntity.ok(service.getAllChargeMasters());
    }

    @PostMapping("/calculate")
    public ResponseEntity<Double> calculate(@RequestBody ChargeMaster cm) {
        log.info("API Call: Calculate Total Amount, Request Body: {}", cm);
        if (cm.getAmount() == null || cm.getGst() == null) {
            log.warn("Amount or GST is null in request body: {}", cm);
            return ResponseEntity.badRequest().build();
        }
        double total = service.calculateTotalAmount(cm.getAmount(), cm.getGst());
        log.info("Calculated Total Amount: {}", total);
        return ResponseEntity.ok(total);
    }

    @PostMapping("/charge/auto")
    public ResponseEntity<?> applyAutoCharge(@RequestBody @Valid AutoChargeMasterRequestDTO request) {
        log.info("API Call: Apply Auto Charge, Request Body: {}", request);
        service.applyAutoCharge(request);
        log.info("Auto charge successfully applied for request: {}", request);
        return ResponseEntity.ok("Auto charge successfully applied.");
    }


}
