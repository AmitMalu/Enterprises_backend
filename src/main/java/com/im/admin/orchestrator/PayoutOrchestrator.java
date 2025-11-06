package com.im.admin.orchestrator;

import com.im.admin.entities.SubProducts;
import com.im.admin.entities.VendorCredentials;
import com.im.admin.entities.VendorRouting;
import com.im.admin.entities.enums.WalletType;
import com.im.admin.entities.vendor.Vendors;
import com.im.admin.repositories.SubProductsRepository;
import com.im.admin.repositories.VendorCredentialsRepository;
import com.im.admin.repositories.VendorRoutingRepository;
import com.im.admin.request.PayoutResponseDto;
import com.im.admin.request.SendPayoutRequest;
import com.im.admin.response.CommonResponse;
import com.im.admin.service.PayoutVendorService;
import com.im.admin.util.CommonMethods;
import com.im.admin.util.VendorFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PayoutOrchestrator {

    @Autowired
    private SubProductsRepository subProductsRepository;

    @Autowired
    private CommonMethods commonMethods;

    @Autowired
    private VendorRoutingRepository vendorRoutingRepository;

    @Autowired
    private VendorCredentialsRepository vendorCredentialsRepository;

    @Autowired
    private VendorFactory vendorFactory;


    @Transactional
    public CommonResponse<PayoutResponseDto> processPayout(SendPayoutRequest req) {
        String orderId = "ORD" + commonMethods.generateBaseTransactionId();
        log.info("Initiating payout process for orderId: {}", orderId);

        try {
            Optional<SubProducts> subProductsOpt = subProductsRepository.findById(req.getSubProductId());
            if (subProductsOpt.isEmpty()) {
                log.warn("SubProduct not found for ID: {}", req.getSubProductId());
                return new CommonResponse<>(404, "SubProduct not found", null);
            }

            SubProducts subProducts = subProductsOpt.get();
            WalletType walletType = WalletType.valueOf(subProducts.getWalletType());
            log.debug("Wallet type resolved: {}", walletType);

            BigDecimal availableBalance = commonMethods.getCurrentBalance(req.getRetailerId(), walletType.name());
            log.debug("Available balance for retailer {}: {}", req.getRetailerId(), availableBalance);

            if (availableBalance.compareTo(req.getAmount()) < 0) {
                log.warn("Insufficient balance for retailer {}. Required: {}, Available: {}", req.getRetailerId(), req.getAmount(), availableBalance);
                return new CommonResponse<>(400, "Insufficient Balance", null);
            }

            VendorRouting routing = vendorRoutingRepository.findBySubProductId(req.getSubProductId());
            if (routing == null) {
                log.error("Vendor routing not found for SubProduct ID: {}", req.getSubProductId());
                return new CommonResponse<>(404, "Vendor routing not found", null);
            }

            Vendors vendor = CommonMethods.getVendors(routing);
            log.debug("Vendor resolved: {}", vendor.getVendorName());

            VendorCredentials credentials = vendorCredentialsRepository
                    .findByVendorIdAndProductId(vendor.getId(), vendor.getProduct().getId())
                    .orElseThrow(() -> {
                        log.error("No credentials found for vendor ID: {}, product ID: {}", vendor.getId(), vendor.getProduct().getId());
                        return new RuntimeException("No credentials found for selected vendor");
                    });

            log.debug("Vendor credentials retrieved for vendor: {}", credentials.getVendor().getVendorName());

            PayoutVendorService vendorService = vendorFactory.getVendor(vendor.getVendorCode());
            if (vendorService == null) {
                log.error("Vendor implementation not found for code: {}", vendor.getVendorCode());
                return new CommonResponse<>(500, "Vendor implementation not found", null);
            }

            log.info("Sending payout request to vendor: {}", vendor.getVendorName());
            PayoutResponseDto vendorResponse = vendorService.sendPayout(req, credentials, orderId, subProducts, walletType);
            log.info("Payout processed successfully for orderId: {}", orderId);

            return new CommonResponse<>(200, "Transaction processed", vendorResponse);

        } catch (Exception e) {
            log.error("Error occurred while processing payout for orderId: {}: {}", orderId, e.getMessage(), e);
            return new CommonResponse<>(500, "Internal Server Error", null);
        }
    }
}
