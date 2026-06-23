package com.im.admin.controller;

import com.im.admin.service.VendorDataSyncService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author SHUBHAM KHOPADE
 */

@RestController
@RequestMapping("/api/data-sync")
public class VendorDataSyncController {

    @Autowired
    private VendorDataSyncService vendorDataSyncService;


    @PostMapping("/vimo-pay/{vendorId}")
    public ResponseEntity<?> syncVendorLists(@PathVariable("vendorId") String vendorId) {
        vendorDataSyncService.fetchAndSaveAll(vendorId);
        return ResponseEntity.ok().body("Sync completed");
    }

}
