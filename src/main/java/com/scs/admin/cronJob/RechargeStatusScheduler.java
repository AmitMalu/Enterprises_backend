package com.scs.admin.cronJob;

import com.scs.admin.dto.TransactionSchemeDetailsRequestDto;
import com.scs.admin.entities.*;
import com.scs.admin.entities.product.Products;
import com.scs.admin.entities.vendor.Vendors;
import com.scs.admin.repositories.*;
import com.scs.admin.service.RechargeVendorService;
import com.scs.admin.service.impl.TransactionEngineServiceImpl;
import com.scs.admin.util.CommonMethods;
import com.scs.admin.util.RechargeVendorFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
public class RechargeStatusScheduler {

    @Autowired
    private RechargeTransactionRepository transactionRepository;

    @Autowired
    private VendorRoutingRepository vendorRoutingRepository;

    @Autowired
    private VendorCredentialsRepository vendorCredentialsRepository;

    @Autowired
    private VendorCredentialsRepository credentialsRepository;

    @Autowired
    private RechargeVendorFactory vendorFactory;
    @Autowired
    private TransactionEngineServiceImpl transactionEngineServiceImpl;
    @Autowired
    private SchemeRateRepository schemeRateRepository;
    @Autowired
    private SchemeRepository schemeRepository;
    @Autowired
    private ProductsRepository productsRepository;
    @Autowired
    private SubProductsRepository subProductsRepository;

    // Runs every 30 seconds
    @Transactional
    @Scheduled(cron = "0 */5 * * * *")
    public void checkPendingTransactions() {

        List<RechargeTransaction> pendingList =
                transactionRepository.findByStatus("PENDING");

        log.info("[RECHARGE_SCHEDULER] Found {} pending transactions", pendingList.size());

        for (RechargeTransaction txn : pendingList) {

            String txnRef = txn.getTransactionRef();
            log.info("[RECHARGE_SCHEDULER] Checking status for TXN_REF={}", txnRef);

            try {

                // 1 Fetch pending transaction
                RechargeTransaction rechargeTxn = getPendingTransaction(txnRef);

                // 2 Fetch scheme rate
                SchemeRate schemeRate = getSchemeRate(rechargeTxn);

                // 3 Fetch scheme
                Scheme scheme = getSchemeEntity(schemeRate.getSchemeCode());

                // 4 Extract product and subproduct
                String[] hashParts = validateAndSplitSchemeHash(scheme.getSchemeCombinationHashCode());

                short productId = Short.parseShort(hashParts[0]);
                String subProductId = hashParts[1];

                // 5 Fetch product
                Products product = getProduct(productId);

                // 6 Fetch subproduct
                SubProducts subProduct = getSubProduct(subProductId);

                // 7 Fetch vendor credentials
                VendorCredentials credentials = getVendorCredentials(subProductId);

                RechargeVendorService vendorService =
                        vendorFactory.getVendor(credentials.getVendor().getVendorName());

                // 8 Call vendor status API
                String vendorResponse =
                        vendorService.checkStatusByTxnRef(
                                rechargeTxn.getTransactionRef(),
                                credentials);

                log.info("[RECHARGE_SCHEDULER] Vendor Response = {}", vendorResponse);

                int statusCode = parseStatusCode(vendorResponse);

                String finalStatus = mapStatus(statusCode);

                if (!"PENDING".equalsIgnoreCase(finalStatus)) {

                    rechargeTxn.setStatus(finalStatus);
                    rechargeTxn.setVendorResponse(vendorResponse);

                    transactionRepository.save(rechargeTxn);

                    // 9 Build transaction DTO
                    TransactionSchemeDetailsRequestDto dto =
                            buildTransactionDTO(
                                    rechargeTxn,
                                    schemeRate,
                                    scheme,
                                    product,
                                    subProduct);

                    //  Call transaction engine
                    transactionEngineServiceImpl.processTransaction(dto, finalStatus);

                    log.info("[RECHARGE_SCHEDULER] Processed TXN_REF={} STATUS={}",
                            txnRef, finalStatus);
                }

            } catch (Exception e) {

                log.error("[RECHARGE_SCHEDULER] Error TXN_REF={} | {}",
                        txnRef, e.getMessage(), e);
            }
        }
    }

    private RechargeTransaction getPendingTransaction(String txnRef) {

        return transactionRepository.findByTransactionRef(txnRef)
                .orElseThrow(() ->
                        new RuntimeException("Transaction not found: " + txnRef));
    }


    private VendorCredentials getVendorCredentials(RechargeTransaction txn) {

        return credentialsRepository
                .findByVendorIdAndProductId(
                        txn.getVendor().getId(),
                        txn.getVendor().getProduct().getId())
                .orElseThrow(() ->
                        new RuntimeException("Vendor credentials not found"));
    }


    private int parseStatusCode(String vendorResponse) {

        String[] parts = vendorResponse.split("=");

        return Integer.parseInt(parts[0]);
    }

    private String mapStatus(int statusCode) {

        return switch (statusCode) {

            case 1 -> "SUCCESS";
            case 2,3 -> "FAILED";
            case 4,6 -> "HOLD";
            case 5 -> "REFUNDED";
            default -> "PENDING";
        };
    }

    private TransactionSchemeDetailsRequestDto buildTransactionDTO(
            RechargeTransaction txn,
            SchemeRate schemeRate,
            Scheme scheme,
            Products product,
            SubProducts subProduct) {

        TransactionSchemeDetailsRequestDto dto = new TransactionSchemeDetailsRequestDto();

        String txnRef = txn.getTransactionRef();
        String baseTxnId = txnRef.startsWith("TXN") ? txnRef.substring(3) : txnRef;

        dto.setBaseTranxId(baseTxnId);
        dto.setAmount(txn.getAmount().doubleValue());
        dto.setConfirmAmount(txn.getAmount().doubleValue());
        dto.setOperatorName(txn.getTransactionType());
        dto.setOperatorCode(schemeRate.getOperatorCode());
        dto.setSchemeRate(schemeRate);
        dto.setTransactionType(product.getTransactionType());
        dto.setWalletType(subProduct.getWalletType());
        dto.setGstValue(String.valueOf(product.getSgst()));
        dto.setTdsRate(String.valueOf(product.getTds()));
        dto.setProductCode(product.getProductCode());
        dto.setSubProductCode(subProduct.getSubProductCode());
        dto.setIsCommission(product.getIsCommissions());
        dto.setHasCharge(product.getHasCharges());
        dto.setHierarchyId(txn.getRetailerId());

        return dto;
    }

    private SchemeRate getSchemeRate(RechargeTransaction txn) {

        return schemeRateRepository
                .findSchemeRateByCodeAndOperatorAndAmount(
                        txn.getSchemeCode(),
                        txn.getTransactionType(),
                        txn.getAmount().doubleValue())
                .orElseThrow(() ->
                        new RuntimeException("No scheme rate found for amount: " + txn.getAmount()));
    }

    private Scheme getSchemeEntity(String schemeCode) {

        return schemeRepository.findBySchemeCode(schemeCode)
                .orElseThrow(() ->
                        new RuntimeException("Scheme not found: " + schemeCode));
    }

    private String[] validateAndSplitSchemeHash(String hash) {

        String[] parts = hash.split("-");

        if (parts.length < 2) {
            throw new RuntimeException("Invalid scheme hash: " + hash);
        }

        return parts;
    }

    private Products getProduct(short productId) {

        return productsRepository.findById(productId)
                .orElseThrow(() ->
                        new RuntimeException("Product not found: " + productId));
    }

    private SubProducts getSubProduct(String subProductId) {

        return subProductsRepository.findById(subProductId)
                .orElseThrow(() ->
                        new RuntimeException("SubProduct not found: " + subProductId));
    }

    private VendorCredentials getVendorCredentials(String subProductId) {

        VendorRouting routing = vendorRoutingRepository.findBySubProductId(subProductId);

        Vendors vendor = CommonMethods.getVendors(routing);

        return vendorCredentialsRepository
                .findByVendorIdAndProductId(vendor.getId(), vendor.getProduct().getId())
                .orElseThrow(() ->
                        new RuntimeException("Vendor credentials not found"));
    }
}