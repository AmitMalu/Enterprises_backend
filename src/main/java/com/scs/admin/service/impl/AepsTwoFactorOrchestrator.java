package com.scs.admin.service.impl;

import com.scs.admin.commons.CommonFunctions;
import com.scs.admin.entities.VendorCredentials;
import com.scs.admin.entities.VendorRouting;
import com.scs.admin.entities.vendor.Vendors;
import com.scs.admin.repositories.AepsMerchantStatusRepository;
import com.scs.admin.repositories.RetailerRepository;
import com.scs.admin.repositories.VendorCredentialsRepository;
import com.scs.admin.repositories.VendorRoutingRepository;
import com.scs.admin.request.Aeps2FaRequest;
import com.scs.admin.response.Aeps2FaResponseDto;
import com.scs.admin.response.CommonResponse;
import com.scs.admin.service.AepsVendorService;
import com.scs.admin.util.AepsVendorFactory;
import com.scs.admin.util.CommonMethods;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class AepsTwoFactorOrchestrator {


    @Autowired
    private RetailerRepository retailerRepository;

    @Autowired
    private ResourceLoader resourceLoader;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private VendorRoutingRepository vendorRoutingRepository;

    @Autowired
    private VendorCredentialsRepository vendorCredentialsRepository;

    @Autowired
    private AepsMerchantStatusRepository aepsMerchantStatusRepository;

    @Autowired
    private CommonFunctions commonFunctions;

    @Autowired private AepsVendorFactory aepsVendorFactory;



    public ResponseEntity<CommonResponse<Aeps2FaResponseDto>> validateAadhar(Aeps2FaRequest req) {

        // Resolve Vendor
        VendorRouting routing =
                vendorRoutingRepository.findBySubProductId(req.getSubProductId());

        Vendors vendor = CommonMethods.getVendors(routing);

        // Get Credentials
        VendorCredentials credentials =
                vendorCredentialsRepository
                        .findByVendorIdAndProductId(
                                vendor.getId(),
                                vendor.getProduct().getId())
                        .orElseThrow(() ->
                                new RuntimeException("Vendor credentials not found"));

        // Get Vendor Implementation
        AepsVendorService vendorService =
                aepsVendorFactory.getVendor(vendor.getVendorName());

        return vendorService.validateAadhar(req, credentials);
    }

}

