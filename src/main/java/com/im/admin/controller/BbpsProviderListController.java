package com.im.admin.controller;

import com.im.admin.dto.BbpsVendorWrapperDTO;
import com.im.admin.service.BbpsProviderListService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/bbps-provider-list")
public class BbpsProviderListController {

    @Autowired
    private BbpsProviderListService bbpsProviderListService;

    @PostMapping("/save-providers")
    public ResponseEntity<String> saveProviders(@RequestBody BbpsVendorWrapperDTO dto) {
        String result = bbpsProviderListService.saveProviderList(dto);
        return ResponseEntity.ok(result);
    }
}
