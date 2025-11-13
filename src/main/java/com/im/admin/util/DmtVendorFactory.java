package com.im.admin.util;

import com.im.admin.service.DmtVendorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class DmtVendorFactory {

    private final Map<String, DmtVendorService> vendorMap = new HashMap<>();

    @Autowired
    public DmtVendorFactory(List<DmtVendorService> vendors) {
        vendors.forEach(v -> vendorMap.put(v.getVendorCode().toLowerCase(), v));
    }

    public DmtVendorService getVendor(String vendorName) {
        if (vendorName == null || vendorName.trim().isEmpty()) {
            return null;
        }

        String normalizedCode = vendorName.trim().toLowerCase();

        // Exact match (case-insensitive)
        if (vendorMap.containsKey(normalizedCode)) {
            return vendorMap.get(normalizedCode);
        }

        // Partial match
        for (Map.Entry<String, DmtVendorService> entry : vendorMap.entrySet()) {
            if (entry.getKey().contains(normalizedCode)) {
                return entry.getValue();
            }
        }

        return null;
    }
}
