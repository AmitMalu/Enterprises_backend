package com.im.admin.orchestrator;

import com.im.admin.entities.VendorCredentials;
import com.im.admin.entities.VendorRouting;
import com.im.admin.entities.vendor.Vendors;
import com.im.admin.request.AepsMerchantRequestDto;
import com.im.admin.response.AepsMerchantResponseDto;
import com.im.admin.response.CommonResponse;
import com.im.admin.service.AepsVendorService;
import com.im.admin.util.CommonMethods;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AepsOrchestrator {

    public CommonResponse<AepsMerchantResponseDto> registerMerchant(AepsMerchantRequestDto req) {

//        // Resolve Vendor
//        VendorRouting routing =
//                vendorRoutingRepository.findBySubProductId(req.getSubProductId());
//
//        Vendors vendor = CommonMethods.getVendors(routing);
//
//        //  Get Credentials
//        VendorCredentials credentials = vendorCredentialsRepository
//                .findByVendorIdAndProductId(vendor.getId(), vendor.getProduct().getId())
//                .orElseThrow(() -> new RuntimeException("Vendor credentials not found"));
//
//        //  Get Vendor Implementation
//        AepsVendorService vendorService =
//                vendorFactory.getVendor(vendor.getVendorName());
//
//        if (vendorService == null) {
//            return new CommonResponse<>(500, "Vendor implementation not found", null);
//        }
//
//        //  Call Vendor Specific Implementation
//        return vendorService.onboardMerchant(req, credentials);
//    }
        return  null;

    }
}
