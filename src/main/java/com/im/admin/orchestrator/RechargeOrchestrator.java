package com.im.admin.orchestrator;

import com.im.admin.commons.CommonFunctions;
import com.im.admin.dto.AbstractWalletAmountTable;
import com.im.admin.entities.*;
import com.im.admin.entities.enums.WalletType;
import com.im.admin.entities.vendor.Vendors;
import com.im.admin.repositories.*;
import com.im.admin.request.RechargeRequest;
import com.im.admin.response.PaysprintSendOtpResponse;
import com.im.admin.response.RechargeResponse;
import com.im.admin.service.DmtVendorService;
import com.im.admin.service.RechargeCallbackHandler;
import com.im.admin.service.RechargeVendorService;
import com.im.admin.util.CommonMethods;
import com.im.admin.util.DmtVendorFactory;
import com.im.admin.util.RechargeVendorFactory;
import com.im.admin.util.TokenUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Service
@Slf4j
public class RechargeOrchestrator {


    @Autowired
    private SubProductsRepository subProductsRepository;

    @Autowired
    private CommonMethods commonMethods;

    @Autowired
    private TokenUtil tokenUtil;

    @Autowired
    private CommonFunctions commonFunctions;


    @Autowired
    private VendorRoutingRepository vendorRoutingRepository;

    @Autowired
    private VendorCredentialsRepository vendorCredentialsRepository;

    @Autowired
    private RechargeTransactionRepository rechargeTransactionRepository;

    @Autowired
    private RechargeCallbackRepository rechargeCallbackRepository;


    @Autowired
    private RechargeVendorFactory vendorFactory;

    @Transactional
    public RechargeResponse recharge(RechargeRequest request, Double latitude, Double longitude) {

        String loggedInHierarchyId = tokenUtil.extractHierarchyId();
        request.setRetailerId(loggedInHierarchyId);

        request.setRechargeType("0");

        String orderId = "TXN" + commonMethods.generateBaseTransactionId();
        log.info("[PROCESS RECHARGE] Starting transaction | orderId={}", orderId);


        // 1 Validate SubProduct
        SubProducts subProducts = subProductsRepository.findById(request.getSubProductId())
                .orElseThrow(() -> new RuntimeException("SubProduct not found"));

        WalletType walletType = WalletType.valueOf(subProducts.getWalletType());

        // 2 Check Retailer Balance
        BigDecimal availableBalance =
                commonMethods.getCurrentBalance(request.getRetailerId(), walletType.name());

        if (availableBalance.compareTo(request.getAmount()) < 0) {
            throw new RuntimeException("Insufficient Balance");
        }

        // 3 Resolve Vendor Routing
        VendorRouting routing =
                vendorRoutingRepository.findBySubProductId(request.getSubProductId());

        Vendors vendor = CommonMethods.getVendors(routing);

        VendorCredentials credentials =
                vendorCredentialsRepository
                        .findByVendorIdAndProductId(
                                vendor.getId(),
                                vendor.getProduct().getId())
                        .orElseThrow(() -> new RuntimeException("Vendor credentials not found"));

        // 4 Get vendor implementation
        RechargeVendorService vendorService =
                vendorFactory.getVendor(vendor.getVendorName());

        // 5 Debit wallet (PENDING)
        AbstractWalletAmountTable wallet =
                commonMethods.getWalletByType(walletType.name(), request.getRetailerId());

        String txnRef = commonMethods.generateBaseTransactionId();

        commonMethods.updateWallet(
                wallet,
                request.getRetailerId(),
                walletType,
                request.getAmount(),
                "Debit",
                txnRef,
                "PENDING",
                "DEBIT",
                subProducts.getSubProductCode(),
                request.getCustomerNumber(),
                "Recharge for " + request.getCustomerNumber(),
                "",
                latitude,
                longitude
        );

        return vendorService.recharge(request,orderId, credentials);

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

        return vendorService.checkBalance(request, credentials);
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

    public String checkStatusDb(RechargeRequest request) {
        RechargeTransaction transaction =
                rechargeTransactionRepository
                        .findByTransactionRef(request.getTransactionRef())
                        .orElseThrow(() -> new RuntimeException("Transaction not found"));

        return transaction.getStatus();
    }

    @Transactional
    public String processRechargeCallback(String vendorCode,
                                          Map<String, String> payload,
                                          HttpServletRequest request) {

        log.info("[RECHARGE_CALLBACK] Vendor={} Payload={}", vendorCode, payload);

        LocalDateTime receivedAt = LocalDateTime.now();
        String clientIp = request.getRemoteAddr();

        try {

            Object vendorService = vendorFactory.getVendor(vendorCode);

            if (!(vendorService instanceof RechargeCallbackHandler handler)) {

                log.error("No recharge callback handler found for vendor={}", vendorCode);
                return "Handler not found";
            }

            String s = handler.handleCallback(payload, request);
            String vendorResponse = handler.handleCallback(payload, request);

            // Log callback event for auditing
            commonFunctions.saveCallbackEventLog(
//                    payload.toString(),
                    payload,
                    receivedAt,
                    clientIp,
                    "Callback processed for vendor = " + vendorCode,
                    LocalDateTime.now()
            );

            return vendorResponse;

        } catch (Exception e) {

            log.error("Recharge callback error {}", e.getMessage(), e);
            return "ERROR";
        }
    }
}
