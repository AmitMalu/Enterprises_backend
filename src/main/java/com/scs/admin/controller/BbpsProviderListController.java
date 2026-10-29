package com.scs.admin.controller;

import com.scs.admin.dto.BbpsVendorWrapperDTO;
import com.scs.admin.service.BbpsProviderListService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/bbps-provider-list")
public class BbpsProviderListController {

    @Autowired
    private BbpsProviderListService bbpsProviderListService;

    @PostMapping("/save-providers")
    public ResponseEntity<String> saveProviders(@RequestBody BbpsVendorWrapperDTO dto) {
        log.info("Received request to save providers: {}", dto);

        String result = bbpsProviderListService.saveProviderList(dto);

        log.info("Provider save result: {}", result);
        return ResponseEntity.ok(result);
    }

}
