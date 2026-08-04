package com.im.admin.orchestrator;

import com.im.admin.commons.CommonFunctions;
import com.im.admin.dto.AbstractWalletAmountTable;
import com.im.admin.dto.RetailersExtraColumnsDto;
import com.im.admin.entities.*;
import com.im.admin.entities.enums.WalletType;
import com.im.admin.entities.vendor.Vendors;
import com.im.admin.repositories.*;
import com.im.admin.request.DthCustomerInfoRequest;
import com.im.admin.request.MobileLookupRequest;
import com.im.admin.request.RechargePlanRequest;
import com.im.admin.request.RechargeRequest;
import com.im.admin.response.*;
import com.im.admin.service.DmtVendorService;
import com.im.admin.service.RechargeCallbackHandler;
import com.im.admin.service.RechargeVendorService;
import com.im.admin.service.impl.SubProductsServiceImpl;
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
    private SchemeRateRepository schemeRateRepository;

    @Autowired
    private VendorRoutingRepository vendorRoutingRepository;

    @Autowired
    private VendorCredentialsRepository vendorCredentialsRepository;

    @Autowired
    private RechargeTransactionRepository rechargeTransactionRepository;

    @Autowired
    private RechargeCallbackRepository rechargeCallbackRepository;

    @Autowired
    private SchemeRepository schemeRepository;

    @Autowired
    private RechargeVendorFactory vendorFactory;

    @Autowired
    private SubProductsServiceImpl subProductsServiceImpl;


    @Transactional
    public RechargeResponse recharge(RechargeRequest request, Double latitude, Double longitude, String ipAddress) {

        String loggedInHierarchyId = tokenUtil.extractHierarchyId();
        request.setRetailerId(loggedInHierarchyId);

        request.setRechargeType("0");

        String orderId = "TXN" + commonMethods.generateBaseTransactionId();
        log.info("[PROCESS RECHARGE] Starting transaction | orderId={}", orderId);

        //Validate Scheme Active or Not
        Scheme scheme = schemeRepository.findBySchemeCode(request.getSchemeCode())
                .orElseThrow(() -> new RuntimeException("Scheme is not activated. Please contact admin"));

        if (!Boolean.TRUE.equals(scheme.getIsActive())) {
            log.warn("Scheme is inactive for schemeCode={}", request.getSchemeCode());
            throw new RuntimeException("Scheme is inactive. Please contact admin");
        }

        // 1 Validate SubProduct
        SubProducts subProducts = subProductsRepository.findById(request.getSubProductId())
                .orElseThrow(() -> new RuntimeException("SubProduct not found"));

        if (!subProductsServiceImpl.isSubProductVisible(subProducts.getId()))
        {
            throw new IllegalArgumentException("Selected sub product is currently not visible.");
        }

        log.info("Sub Product is visible and available for wallet transfer. subProductId={}", subProducts.getId());

        WalletType walletType = WalletType.valueOf(subProducts.getWalletType());

        String transactionType =
                request.getOperatorName() + "(" + request.getOperatorCode() + ")";

        // 2 Check Retailer Balance
        BigDecimal availableBalance =
                commonMethods.getCurrentBalance(request.getRetailerId(), walletType.name());

        SchemeRate selectedScheme = schemeRateRepository
                .findSchemeRateByCodeAndOperatorAndAmount(
                        request.getSchemeCode(),
                        transactionType,
                        request.getAmount().doubleValue())
                .orElseThrow(() -> new RuntimeException("No matching scheme rate found for amount: " + request.getAmount()));

        commonMethods.validateDebitTransaction(
                request.getRetailerId(),
                walletType,
                availableBalance,
                request.getAmount(),
                BigDecimal.valueOf(selectedScheme.getGross())
        );

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

        String baseTxnId = orderId.startsWith("TXN") ? orderId.substring(3) : orderId;

        RetailersExtraColumnsDto retailerExtraColumns =
                RetailersExtraColumnsDto.builder()
                        .medium(request.getMedium())
                        .clientIp(ipAddress)
                        .requestDomainName(request.getRequestDomainName())
                        .latitude(latitude)
                        .longitude(longitude)
                        .build();

        commonMethods.updateWallet(
                wallet,
                request.getRetailerId(),
                walletType,
                request.getAmount(),
                "Debit",
                baseTxnId,
                "PENDING",
                "DEBIT",
                subProducts.getSubProductCode(),
                request.getCustomerNumber(),
                "Recharge for " + request.getCustomerNumber(),
                selectedScheme.getSchemeCode(),
                retailerExtraColumns,
                vendor.getVendorName(),
                selectedScheme.getOperatorName()
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
                    payload.toString(),
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

    public MobileLookupResponse mobileLookup(MobileLookupRequest request) {

        VendorRouting routing =
                vendorRoutingRepository.findBySubProductId(request.getSubProductId()); // or config-based

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

        return vendorService.mobileLookup(request, credentials);
    }

    public OperatorResponse getOperatorCodes(MobileLookupRequest request) {

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

        return vendorService.getOperatorCodes(credentials);
    }

    public RechargePlanResponse getRechargePlans(RechargePlanRequest request) {

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

        return vendorService.getRechargePlans(request, credentials);
    }

    public DthCustomerInfoResponse getDthCustomerInfo(
            DthCustomerInfoRequest request) {

        VendorRouting routing =
                vendorRoutingRepository
                        .findBySubProductId(request.getSubProductId());

        Vendors vendor =
                CommonMethods.getVendors(routing);

        VendorCredentials credentials =
                vendorCredentialsRepository
                        .findByVendorIdAndProductId(
                                vendor.getId(),
                                vendor.getProduct().getId())
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Vendor credentials not found"));

        RechargeVendorService vendorService =
                vendorFactory.getVendor(
                        vendor.getVendorName());

        return vendorService.getDthCustomerInfo(
                request,
                credentials);
    }
}
