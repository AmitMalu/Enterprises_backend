package com.im.admin.util;

import com.im.admin.service.PayoutVendorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class PayoutVendorFactory {

    private final Map<String, PayoutVendorService> vendorMap = new HashMap<>();

    @Autowired
    public PayoutVendorFactory(List<PayoutVendorService> vendors) {
        vendors.forEach(v -> vendorMap.put(v.getVendorName().toLowerCase(), v));
    }

    public PayoutVendorService getVendor(String vendorName) {
        if (vendorName == null || vendorName.trim().isEmpty()) {
            return null;
        }

        String normalizedCode = vendorName.trim().toLowerCase();

        // Exact match (case-insensitive)
        if (vendorMap.containsKey(normalizedCode)) {
            return vendorMap.get(normalizedCode);
        }

        // Partial match
        for (Map.Entry<String, PayoutVendorService> entry : vendorMap.entrySet()) {
//            if (entry.getKey().contains(normalizedCode)) {
//                return entry.getValue();
//            }
            if (normalizedCode.contains(entry.getKey())) {
                return entry.getValue();
            }

        }

        return null;
    }
}

