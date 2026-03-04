package com.im.admin.orchestrator;

import com.im.admin.entities.VendorCredentials;
import com.im.admin.entities.VendorRouting;
import com.im.admin.entities.vendor.Vendors;
import com.im.admin.repositories.VendorCredentialsRepository;
import com.im.admin.repositories.VendorRoutingRepository;
import com.im.admin.request.RechargeRequest;
import com.im.admin.response.PaysprintSendOtpResponse;
import com.im.admin.response.RechargeResponse;
import com.im.admin.service.DmtVendorService;
import com.im.admin.service.RechargeVendorService;
import com.im.admin.util.CommonMethods;
import com.im.admin.util.DmtVendorFactory;
import com.im.admin.util.RechargeVendorFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RechargeOrchestrator {

    @Autowired
    private VendorRoutingRepository vendorRoutingRepository;

    @Autowired
    private VendorCredentialsRepository vendorCredentialsRepository;

    @Autowired
    private RechargeVendorFactory vendorFactory;

    public RechargeResponse recharge(RechargeRequest request) {

        VendorRouting routing = vendorRoutingRepository.findBySubProductId(request.getSubProductId());
        Vendors vendor = CommonMethods.getVendors(routing);

        VendorCredentials credentials = vendorCredentialsRepository
                .findByVendorIdAndProductId(vendor.getId(), vendor.getProduct().getId())
                .orElseThrow(() -> new RuntimeException("Vendor credentials not found"));

        RechargeVendorService vendorService = vendorFactory.getVendor(vendor.getVendorName());

        RechargeResponse recharge = vendorService.recharge(request, credentials);

        return  recharge;

    }

    public String checkBalance(RechargeRequest request) {

        VendorRouting routing =
                vendorRoutingRepository.findBySubProductId(request.getSubProductId());

        Vendors vendor = CommonMethods.getVendors(routing);

        VendorCredentials credentials =
                vendorCredentialsRepository
                        .findByVendorIdAndProductId(
                                vendor.getId(),
                                vendor.getProduct().getId())
                        .orElseThrow(() ->
                                new RuntimeException("Vendor credentials not found"));

        RechargeVendorService vendorService =
                vendorFactory.getVendor(vendor.getVendorName());

        return vendorService.checkBalance(request,credentials);
    }


    public String checkStatus(RechargeRequest request) {

        VendorRouting routing =
                vendorRoutingRepository.findBySubProductId(request.getSubProductId());

        Vendors vendor = CommonMethods.getVendors(routing);

        VendorCredentials credentials =
                vendorCredentialsRepository
                        .findByVendorIdAndProductId(
                                vendor.getId(),
                                vendor.getProduct().getId())
                        .orElseThrow(() ->
                                new RuntimeException("Vendor credentials not found"));

        RechargeVendorService vendorService =
                vendorFactory.getVendor(vendor.getVendorName());

        return vendorService.checkStatus(request, credentials);
    }
}
