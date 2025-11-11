//package com.im.admin.service.impl;
//
//import com.im.admin.dto.AbstractWalletAmountTable;
//import com.im.admin.dto.DmtResponseDto;
//import com.im.admin.dto.RemitterTransactionResponse;
//import com.im.admin.entities.SubProducts;
//import com.im.admin.entities.VendorCredentials;
//import com.im.admin.entities.VendorRouting;
//import com.im.admin.entities.enums.WalletType;
//import com.im.admin.entities.vendor.Vendors;
//import com.im.admin.repositories.SubProductsRepository;
//import com.im.admin.request.SendDmtRequest;
//import com.im.admin.response.CommonResponse;
//import com.im.admin.service.PayoutVendorService;
//import com.im.admin.service.PaysprintDmtService;
//import com.im.admin.util.CommonMethods;
//import com.im.admin.util.VendorFactory;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Service;
//
//import java.math.BigDecimal;
//
//@Slf4j
//@Service
//public class PaysprintDmtServiceImpl implements PaysprintDmtService {
//
//    @Autowired
//    private CommonMethods commonMethods;
//
//    @Autowired
//    private VendorFactory vendorFactory;
//
//    @Autowired
//    private SubProductsRepository subProductsRepository;
//
//    // You also need to autowire these if you're using them
//    // (otherwise you'll get NullPointerException)
//    // @Autowired
//    // private VendorRoutingRepository vendorRoutingRepository;
//    // @Autowired
//    // private VendorCredentialsRepository vendorCredentialsRepository;
//
//    @Override
//    public CommonResponse<DmtResponseDto> processDmt(SendDmtRequest req) {
//        String orderId = "DMT" + commonMethods.generateBaseTransactionId();
//        log.info("[PROCESS DMT] Starting transaction | orderId={}", orderId);
//
//        try {
//            // 1️⃣ Validate SubProduct
//            SubProducts subProducts = subProductsRepository.findById(req.getSubProductId())
//                    .orElseThrow(() -> new RuntimeException("SubProduct not found for ID: " + req.getSubProductId()));
//
//            WalletType walletType = WalletType.valueOf(subProducts.getWalletType());
//            log.debug("[PROCESS DMT] Resolved walletType={}", walletType);
//
//            // 2️⃣ Check Retailer Balance
//            BigDecimal availableBalance = commonMethods.getCurrentBalance(req.getRetailerId(), walletType.name());
//            if (availableBalance.compareTo(req.getAmount()) < 0) {
//                log.warn("[PROCESS DMT] Insufficient balance for retailer {} (required={}, available={})",
//                        req.getRetailerId(), req.getAmount(), availableBalance);
//                return new CommonResponse<>(400, "Insufficient Balance", null);
//            }
//
//            // 3️⃣ Resolve Vendor Routing & Credentials
//            VendorRouting routing = vendorRoutingRepository.findBySubProductId(req.getSubProductId());
//            if (routing == null) {
//                log.error("[PROCESS DMT] Vendor routing not found for subProductId={}", req.getSubProductId());
//                return new CommonResponse<>(404, "Vendor routing not found", null);
//            }
//
//            Vendors vendor = CommonMethods.getVendors(routing);
//            VendorCredentials credentials = vendorCredentialsRepository
//                    .findByVendorIdAndProductId(vendor.getId(), vendor.getProduct().getId())
//                    .orElseThrow(() -> new RuntimeException("Vendor credentials not found"));
//
//            // 4️⃣ Get Vendor Service Bean (Paysprint / Pana)
//            PayoutVendorService vendorService = vendorFactory.getVendor(vendor.getVendorName());
//            if (vendorService == null) {
//                log.error("[PROCESS DMT] No vendor implementation found for vendorName={}", vendor.getVendorName());
//                return new CommonResponse<>(500, "Vendor implementation not found", null);
//            }
//
//            // 5️⃣ Wallet Debit (PENDING)
//            AbstractWalletAmountTable walletAmountTable =
//                    commonMethods.getWalletByType(walletType.name(), req.getRetailerId());
//            String baseTxnId = orderId.substring(3);
//
//            commonMethods.updateWallet(
//                    walletAmountTable,
//                    req.getRetailerId(),
//                    walletType,
//                    req.getAmount(),
//                    "Debit",
//                    baseTxnId,
//                    "PENDING",
//                    "DEBIT",
//                    subProducts.getSubProductTitle(),
//                    req.getBeneficiaryName() + " #" + req.getAccountNumber(),
//                    "Fund Transferred #" + req.getAccountNumber()
//            );
//
//            log.debug("[PROCESS DMT] Wallet debited (PENDING) for retailer={}, amount={}",
//                    req.getRetailerId(), req.getAmount());
//
//            // 6️⃣ CALL Vendor API
//            log.info("[PROCESS DMT] Calling vendor API for orderId={}, vendor={}",
//                    orderId, vendor.getVendorName());
//
//            RemitterTransactionResponse vendorResponse = vendorService.doTransaction(req, credentials);
//
//            // ✅ Finally return success response
//            return new CommonResponse<>(200, "Transaction Initiated Successfully", new DmtResponseDto());
//
//        } catch (Exception e) {
//            log.error("[PROCESS DMT] Exception occurred: {}", e.getMessage(), e);
//            return new CommonResponse<>(500, "Error while processing DMT", null);
//        }
//    }
//}
//
//
