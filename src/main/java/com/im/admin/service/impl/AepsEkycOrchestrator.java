package com.im.admin.service.impl;

import com.im.admin.commons.CommonFunctions;
import com.im.admin.entities.VendorCredentials;
import com.im.admin.entities.VendorRouting;
import com.im.admin.entities.vendor.Vendors;
import com.im.admin.repositories.AepsMerchantStatusRepository;
import com.im.admin.repositories.RetailerRepository;
import com.im.admin.repositories.VendorCredentialsRepository;
import com.im.admin.repositories.VendorRoutingRepository;
import com.im.admin.request.*;
import com.im.admin.response.*;
import com.im.admin.service.AepsVendorService;
import com.im.admin.util.AepsVendorFactory;
import com.im.admin.util.CommonMethods;
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
