package com.im.admin.controller;

import com.im.admin.entities.WhiteLabels;
import com.im.admin.service.WhiteLabelService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api/white-labels")
@Slf4j
@RequiredArgsConstructor
public class WhiteLabelController {

    @Autowired
    private WhiteLabelService whiteLabelService;

    @PostMapping("/save")
    public ResponseEntity<WhiteLabels> saveWhiteLabel(@Valid @RequestBody WhiteLabels whiteLabel) {
        log.info("Saving WhiteLabel: {}", whiteLabel);
        return ResponseEntity.ok(whiteLabelService.saveWhiteLabel(whiteLabel));
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<WhiteLabels> updateWhiteLabel(@PathVariable String id, @Valid @RequestBody WhiteLabels whiteLabel) {
        log.info("Updating WhiteLabel: {}", id);
        return ResponseEntity.ok(whiteLabelService.updateWhiteLabel(id, whiteLabel));
    }

    @GetMapping("/getAll")
    public ResponseEntity<List<WhiteLabels>> getAllWhiteLabels() {
        log.info("Fetching all WhiteLabels");
        return ResponseEntity.ok(whiteLabelService.getAllWhiteLabels());
    }

    @GetMapping("/getById/{id}")
    public ResponseEntity<WhiteLabels> getWhiteLabelById(@PathVariable String id) {
        log.info("Fetching WhiteLabel by ID: {}", id);
        return ResponseEntity.ok(whiteLabelService.getWhiteLabelById(id));
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<String> deleteWhiteLabel(@PathVariable String id) {
        log.info("Deleting WhiteLabel: {}", id);
        whiteLabelService.deleteWhiteLabel(id);
        return ResponseEntity.ok("WhiteLabel " + id + " deleted successfully.");
    }
}


