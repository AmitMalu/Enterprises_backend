package com.im.admin.util;


import com.im.admin.service.RechargeVendorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class RechargeVendorFactory {

    private final Map<String, RechargeVendorService> vendorMap = new HashMap<>();

    @Autowired
    public RechargeVendorFactory(List<RechargeVendorService> vendors) {
        vendors.forEach(v -> vendorMap.put(v.getVendorCode().toLowerCase(), v));
    }

    public RechargeVendorService getVendor(String vendorName) {
        if (vendorName == null || vendorName.trim().isEmpty()) {
            return null;
        }

        String code = vendorName.trim().toLowerCase();

        if (vendorMap.containsKey(code)) {
            return vendorMap.get(code);
        }

        for (String key : vendorMap.keySet()) {
            if (key.contains(code)) {
                return vendorMap.get(key);
            }
        }

        return null;
    }
}



