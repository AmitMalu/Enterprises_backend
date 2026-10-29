package com.scs.admin.service.impl;

import com.scs.admin.commons.CommonFunctions;
import com.scs.admin.entities.VendorCredentials;
import com.scs.admin.entities.VendorRouting;
import com.scs.admin.entities.vendor.Vendors;
import com.scs.admin.repositories.AepsMerchantStatusRepository;
import com.scs.admin.repositories.RetailerRepository;
import com.scs.admin.repositories.VendorCredentialsRepository;
import com.scs.admin.repositories.VendorRoutingRepository;
import com.scs.admin.request.*;
import com.scs.admin.response.*;
import com.scs.admin.service.AepsVendorService;
import com.scs.admin.util.AepsVendorFactory;
import com.scs.admin.util.CommonMethods;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class AepsEkycOrchestrator {
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

    public ResponseEntity<CommonResponse<AepsEkycResponseDto>> sendOtp(AepsSendOtpRequest req) {
        // Resolve Vendor
        VendorRouting routing =
                vendorRoutingRepository.findBySubProductId(req.getSubProductId());

        Vendors vendor = CommonMethods.getVendors(routing);

        // Get Credentials
        VendorCredentials credentials = vendorCredentialsRepository
                .findByVendorIdAndProductId(vendor.getId(), vendor.getProduct().getId())
                .orElseThrow(() -> new RuntimeException("Vendor credentials not found"));

        // Get Vendor Implementation
        AepsVendorService vendorService =
                aepsVendorFactory.getVendor(vendor.getVendorName());

        return vendorService.sendOtp(req, credentials);
}


    public ResponseEntity<AepsValidateOtpResponseDto> validateOtp(AepsValidateOtpRequest req) {

        // Resolve Vendor
        VendorRouting routing =
                vendorRoutingRepository.findBySubProductId(req.getSubProductId());

        Vendors vendor = CommonMethods.getVendors(routing);

        // Get Credentials
        VendorCredentials credentials = vendorCredentialsRepository
                .findByVendorIdAndProductId(vendor.getId(), vendor.getProduct().getId())
                .orElseThrow(() -> new RuntimeException("Vendor credentials not found"));

        // Get Vendor Implementation
        AepsVendorService vendorService =
                aepsVendorFactory.getVendor(vendor.getVendorName());

        return vendorService.validateOtp(req, credentials);
    }

    public ResponseEntity<CommonResponse<AepsResendOtpResponseDto>> resendOtp(AepsResendOtpRequest req) {

        // Resolve Vendor
        VendorRouting routing =
                vendorRoutingRepository.findBySubProductId(req.getSubProductId());

        Vendors vendor = CommonMethods.getVendors(routing);

        // Get Credentials
        VendorCredentials credentials = vendorCredentialsRepository
                .findByVendorIdAndProductId(vendor.getId(), vendor.getProduct().getId())
                .orElseThrow(() -> new RuntimeException("Vendor credentials not found"));

        // Get Vendor Implementation
        AepsVendorService vendorService =
                aepsVendorFactory.getVendor(vendor.getVendorName());

        return vendorService.resendOtp(req, credentials);
    }


    public ResponseEntity<AepsBiometricEkycResponseDto> biometricEkyc(AepsBiometricEkycRequest req) {

        // Resolve Vendor
        VendorRouting routing =
                vendorRoutingRepository.findBySubProductId(req.getSubProductId());

        Vendors vendor = CommonMethods.getVendors(routing);

        // Get Credentials
        VendorCredentials credentials = vendorCredentialsRepository
                .findByVendorIdAndProductId(vendor.getId(), vendor.getProduct().getId())
                .orElseThrow(() -> new RuntimeException("Vendor credentials not found"));

        // Get Vendor Implementation
        AepsVendorService vendorService =
                aepsVendorFactory.getVendor(vendor.getVendorName());

        return vendorService.biometricEkyc(req, credentials);
    }


    public ResponseEntity<CommonResponse<AepsBiometricEkycResponseDto>> validateOtpThenBiometric(
            AepsCombinedRequest req) {

        // Resolve Vendor
        VendorRouting routing =
                vendorRoutingRepository.findBySubProductId(req.getSubProductId());

        Vendors vendor = CommonMethods.getVendors(routing);

        // Get Credentials
        VendorCredentials credentials = vendorCredentialsRepository
                .findByVendorIdAndProductId(vendor.getId(), vendor.getProduct().getId())
                .orElseThrow(() -> new RuntimeException("Vendor credentials not found"));

        // Get Vendor Implementation
        AepsVendorService vendorService =
                aepsVendorFactory.getVendor(vendor.getVendorName());

        return vendorService.validateOtpThenBiometric(req, credentials);
    }

}
