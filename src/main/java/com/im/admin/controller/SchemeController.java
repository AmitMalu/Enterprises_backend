package com.im.admin.controller;


import com.im.admin.dto.SchemeDTO;
import com.im.admin.service.SchemeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/scheme")
public class SchemeController {

    @Autowired
    private SchemeService schemeService;

    @GetMapping("/hierarchy-list")
    public ResponseEntity<List<Map<String, Object>>> getAllSchemeHierarchySelections() {
        return ResponseEntity.ok(schemeService.getAllSchemeHierarchySelections());
    }

    @GetMapping("/search")
    public ResponseEntity<List<Map<String, Object>>> searchSchemeHierarchy(@RequestParam String keyword) {
        return ResponseEntity.ok(schemeService.searchSchemeHierarchy(keyword));
    }

    @PostMapping("/save")
    public ResponseEntity<Map<String, Object>> saveScheme(@RequestBody SchemeDTO schemeDTO) {
        return ResponseEntity.ok(schemeService.saveScheme(schemeDTO));
    }

}
