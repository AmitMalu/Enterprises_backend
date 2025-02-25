package com.im.admin.controller;

import com.im.admin.entities.WhiteLabels;
import com.im.admin.service.WhiteLabelsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;


@RestController
@RequestMapping("/api/white-labels")
public class WhiteLabelsController {

    @Autowired
    private WhiteLabelsService whiteLabelsService;

    @PostMapping
    public ResponseEntity<WhiteLabels> saveWhiteLabel(@RequestBody WhiteLabels whiteLabels) {
        WhiteLabels savedWhiteLabel = whiteLabelsService.saveWhiteLabel(whiteLabels);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedWhiteLabel);
    }

    @PutMapping("/{id}")
    public ResponseEntity<WhiteLabels> updateWhiteLabel(@PathVariable Long id, @RequestBody WhiteLabels whiteLabels) {
        WhiteLabels updatedWhiteLabel = whiteLabelsService.updateWhiteLabel(id, whiteLabels);
        return ResponseEntity.ok(updatedWhiteLabel);
    }

    @GetMapping("/{id}")
    public ResponseEntity<WhiteLabels> getWhiteLabelById(@PathVariable Long id) {
        WhiteLabels whiteLabel = whiteLabelsService.getWhiteLabelById(id);
        return ResponseEntity.ok(whiteLabel);
    }

    @GetMapping
    public ResponseEntity<Page<WhiteLabels>> getAllWhiteLabels(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<WhiteLabels> whiteLabels = whiteLabelsService.getAllWhiteLabels(page, size);
        return ResponseEntity.ok(whiteLabels);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteWhiteLabel(@PathVariable Long id) {
        whiteLabelsService.deleteWhiteLabel(id);
        return ResponseEntity.noContent().build();
    }
}

