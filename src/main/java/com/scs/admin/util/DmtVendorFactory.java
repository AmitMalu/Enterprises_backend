package com.scs.admin.util;

import com.scs.admin.service.DmtVendorService;
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

        //  Store normalized keys
        vendors.forEach(v ->
                vendorMap.put(normalize(v.getVendorCode()), v)
        );

        // DEBUG (optional - remove later)
        System.out.println("VENDOR MAP KEYS: " + vendorMap.keySet());
    }

    public DmtVendorService getVendor(String vendorName) {

        if (vendorName == null || vendorName.trim().isEmpty()) {
            return null;
        }

        String normalizedCode = normalize(vendorName);

        // DEBUG (optional - remove later)
        System.out.println("SEARCH KEY: " + normalizedCode);

        //  Direct match
        if (vendorMap.containsKey(normalizedCode)) {
            return vendorMap.get(normalizedCode);
        }

        //  Safe fallback (ignore spaces completely)
        for (Map.Entry<String, DmtVendorService> entry : vendorMap.entrySet()) {

            String mapKey = entry.getKey().replaceAll("\\s+", "");
            String inputKey = normalizedCode.replaceAll("\\s+", "");

            if (mapKey.equals(inputKey)) {
                return entry.getValue();
            }
        }

        return null;
    }

    //  COMMON NORMALIZATION METHOD
    private String normalize(String value) {
        return value
                .trim()                    // remove start/end spaces
                .replaceAll("\\s+", " ")  // remove extra spaces
                .toLowerCase();           // lowercase
    }

}
