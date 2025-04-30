package com.im.admin.controller;

import com.im.admin.dto.AutoChargeMasterRequestDTO;
import com.im.admin.entities.ChargeMaster;
import com.im.admin.service.ChargeMasterService;
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
        log.info("API Call: Create ChargeMaster");
        return ResponseEntity.ok(service.createChargeMaster(cm));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ChargeMaster> update(@PathVariable Long id, @Valid @RequestBody ChargeMaster cm) {
        log.info("API Call: Update ChargeMaster");
        return ResponseEntity.ok(service.updateChargeMaster(id, cm));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        log.info("API Call: Delete ChargeMaster");
        service.deleteChargeMaster(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ChargeMaster> getById(@PathVariable Long id) {
        log.info("API Call: Get ChargeMaster by ID");
        return ResponseEntity.ok(service.getChargeMaster(id));
    }

    @GetMapping
    public ResponseEntity<List<ChargeMaster>> getAll() {
        log.info("API Call: Get all ChargeMasters");
        return ResponseEntity.ok(service.getAllChargeMasters());
    }

    @PostMapping("/calculate")
    public ResponseEntity<Double> calculate(@RequestBody ChargeMaster cm) {
        log.info("API Call: Calculate Total Amount");
        if (cm.getAmount() == null || cm.getGst() == null) {
            return ResponseEntity.badRequest().build();
        }
        double total = service.calculateTotalAmount(cm.getAmount(), cm.getGst());
        return ResponseEntity.ok(total);
    }

    @PostMapping("/charge/auto")
    public ResponseEntity<?> applyAutoCharge(@RequestBody @Valid AutoChargeMasterRequestDTO request) {
        service.applyAutoCharge(request);
        return ResponseEntity.ok("Auto charge successfully applied.");
    }

}
