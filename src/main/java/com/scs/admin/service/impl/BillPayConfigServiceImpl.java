package com.scs.admin.service.impl;

import com.scs.admin.entities.BillPayConfig;
import com.scs.admin.entities.VendorRouting;
import com.scs.admin.entities.vendor.Vendors;
import com.scs.admin.repositories.BillPayConfigRepository;
import com.scs.admin.repositories.VendorRepository;
import com.scs.admin.repositories.VendorRoutingRepository;
import com.scs.admin.service.BillPayConfigService;
import com.scs.admin.util.CommonMethods;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@Service
public class BillPayConfigServiceImpl implements BillPayConfigService {

    @Autowired
    private BillPayConfigRepository billPayConfigRepository;

    @Autowired
    private VendorRoutingRepository vendorRoutingRepository;

    @Override
    public BillPayConfig saveConfig(BillPayConfig config) {

        if (config.getRawConfig() == null || config.getRawConfig().trim().isEmpty()) {
            config.setRawConfig("{}"); // default empty JSON
        }


        return billPayConfigRepository.save(config);
    }

    @Override
    public List<String> getServiceNamesBySubProduct(String subProductId) {

        // STEP 1: Find vendor routing
        VendorRouting routing =
                vendorRoutingRepository.findBySubProductId(subProductId);

        // STEP 2: Convert to vendor enum
        Vendors vendor = CommonMethods.getVendors(routing);

        // STEP 4: SQL Equivalent:
        // SELECT DISTINCT service_name FROM vendor_mapping WHERE vendor = :vendor
        return billPayConfigRepository.findDistinctByVendor(vendor.getVendorName());
    }

    @Override
    public List<Object[]> getProviders(String subProductId, String serviceName) {

        // STEP 1: Find routing
        VendorRouting routing = vendorRoutingRepository.findBySubProductId(subProductId);

        if (routing == null) {
            throw new RuntimeException("No routing found for subProductId: " + subProductId);
        }

        // STEP 2: Convert routing → vendor enum
        Vendors vendor = CommonMethods.getVendors(routing);

        // STEP 3: Fetch provider list by vendor + serviceName
        return billPayConfigRepository.findProviders(vendor.getVendorName(), serviceName);
    }


    @Override
    public String getRawConfig(String subProductId, String providerId) {

        // STEP 1: Find routing for vendor
        VendorRouting routing = vendorRoutingRepository.findBySubProductId(subProductId);

        if (routing == null) {
            throw new RuntimeException("Routing not found for subProductId: " + subProductId);
        }

        // STEP 2: Convert routing → vendor enum
        Vendors vendor = CommonMethods.getVendors(routing);

        // STEP 3: Fetch raw config
        return billPayConfigRepository.findRawConfig(vendor.getVendorName(), providerId);
    }



}
