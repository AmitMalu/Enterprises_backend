package com.im.admin.util;

import com.im.admin.service.PayoutVendorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class VendorFactory {

    private final Map<String, PayoutVendorService> vendorMap = new HashMap<>();

    @Autowired
    public VendorFactory(List<PayoutVendorService> vendors) {
        vendors.forEach(v -> vendorMap.put(v.getVendorCode().toLowerCase(), v));
    }

    public PayoutVendorService getVendor(String vendorCode) {
        if (vendorCode == null || vendorCode.trim().isEmpty()) {
            return null;
        }

        String normalizedCode = vendorCode.trim().toLowerCase();

        // Exact match (case-insensitive)
        if (vendorMap.containsKey(normalizedCode)) {
            return vendorMap.get(normalizedCode);
        }

        // Partial match
        for (Map.Entry<String, PayoutVendorService> entry : vendorMap.entrySet()) {
            if (entry.getKey().contains(normalizedCode)) {
                return entry.getValue();
            }
        }

        // Fallback: return null or a default vendor
        return null;
    }
}

