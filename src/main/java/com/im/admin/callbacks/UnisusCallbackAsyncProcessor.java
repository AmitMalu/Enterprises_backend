package com.im.admin.callbacks;

import com.im.admin.dto.TransactionSchemeDetailsRequestDto;
import com.im.admin.entities.*;
import com.im.admin.entities.enums.SlabType;
import com.im.admin.entities.product.Products;
import com.im.admin.repositories.*;
import com.im.admin.request.UnisusWebhookRequest;
import com.im.admin.service.impl.TransactionEngineServiceImpl;
import com.im.admin.util.CommonMethods;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class UnisusCallbackAsyncProcessor {

    private final PgTransactionMappingRepository pgTransactionMappingRepository;
    private final TransactionEngineServiceImpl transactionServiceImpl;
    private final SubProductsRepository subProductsRepository;
    private final SchemeAllocationRepository schemeAllocationRepository;
    private final SchemeRateRepository schemeRateRepository;
    private final SchemeRepository schemeRepository;
    private final ProductsRepository productsRepository;
    private final CommonMethods commonMethods;

    @Async
    public void processAsync(UnisusWebhookRequest request) {

        String trnRefNo =
                request.getTransaction().getTrnRefNo();

        try {

            PgTransactionMapping entity =
                    pgTransactionMappingRepository.findByOrderId(trnRefNo)
                            .orElseThrow(() ->
                                    new RuntimeException("Transaction not found"));

            int status = request.getTransaction().getStatus();
            String vendorStatus = "FAILED";

            if (status == 1) {
                entity.setStatus("SUCCESS");
                vendorStatus = "SUCCESS";
            } else if (status == 2) {
                entity.setStatus("FAILED");
                vendorStatus = "FAILED";
            } else {
                entity.setStatus("PENDING");
                vendorStatus = "PENDING";
            }

            pgTransactionMappingRepository.save(entity);

            log.info("---- [Callback] Processing for Order ID: {}, Status: {}", trnRefNo, vendorStatus);

            // Fetch base transaction mapping
            log.info("---- [Callback] Fetching transaction mapping for Order ID: {}", trnRefNo);
            PgTransactionMapping pgTransactionMapping = pgTransactionMappingRepository.findByOrderId(trnRefNo)
                    .orElseThrow(() -> new RuntimeException("Transaction not found for Order ID: " + trnRefNo));

            String subProductId = pgTransactionMapping.getSubProductId();
            log.info("---- [Callback] Fetching SubProduct using subProductId: {}", subProductId);

            SubProducts subProducts =
                    subProductsRepository.findById(subProductId)
                            .orElseThrow(() -> {
                                log.error("---- [Callback] SubProduct NOT FOUND for subProductId: {}", subProductId);
                                return new RuntimeException("SubProduct not found for subProductId: " + subProductId);
                            });

            log.info("---- [Callback] SubProduct fetched successfully | subProductId={}, subProductCode={}",
                    subProducts.getId(),
                    subProducts.getSubProductCode());

            SchemeAllocation schemeAllocation =
                    schemeAllocationRepository
                            .findByHierarchyIdAndSubProductCode(
                                    pgTransactionMapping.getInitiatedBy(),
                                    subProducts.getSubProductCode()
                            )
                            .orElseThrow(() -> new RuntimeException(
                                    "SchemeAllocation not found | hierarchyId="
                                            + pgTransactionMapping.getInitiatedBy()
                                            + ", subProductCode="
                                            + subProducts.getSubProductCode()
                            ));

            String schemeCode = schemeAllocation.getSchemeCode();

            log.info("---- [Callback] SchemeAllocation fetched successfully | schemeCode={}", schemeCode);

            // Prepare base data
            Double txnAmt = request.getTransaction().getAmount();
            log.info("---- [Callback] Parsed transaction amount: {}", txnAmt);

//            String callbackOperatorName = toDbOperatorName(
//                    request.getOtherInfo().getPGTrnType()
//            );
            log.info("---- [Callback] Using operatorName for DB lookup: {}", request.getOtherInfo().getPgTrnType());

            List<SchemeRate> matchingSchemes =
                    schemeRateRepository.findBySchemeCodeAndOperatorName(
                            schemeCode,
                            request.getOtherInfo().getPgTrnType()+"_"+request.getOtherInfo().getPgName()
                    );
            if (matchingSchemes == null || matchingSchemes.isEmpty()) {
                log.error("---- [Callback] No matching scheme found for schemeCode={} and operatorName={}",
                        schemeCode, request.getOtherInfo().getPgTrnType());
                return;
            }

            log.info("---- [Callback] Scheme(s) found: {}", matchingSchemes.size());

            // Pick correct scheme by amount range or fixed value
            SchemeRate selectedScheme = matchingSchemes.stream()
                    .filter(scheme ->
                            scheme.getSlabType() == SlabType.VARIABLE &&
                                    txnAmt >= scheme.getSlabStart() && txnAmt <= scheme.getSlabEnd()
                    )
                    .findFirst()
                    .orElse(matchingSchemes.get(0));
            log.info("---- [Callback] Selected scheme: {}", selectedScheme);

            // Fetch scheme, product & subproduct details
            log.debug("---- [Callback] Fetching scheme details for code: {}", selectedScheme.getSchemeCode());
            log.info("---- [Callback] Fetching scheme details for code: {}", selectedScheme.getSchemeCode());
            Scheme scheme = schemeRepository.findBySchemeCode(selectedScheme.getSchemeCode())
                    .orElseThrow(() -> new RuntimeException("Scheme Not Found for code: " + selectedScheme.getSchemeCode()));

            String[] hashParts = scheme.getSchemeCombinationHashCode().split("-");
            short productId = Short.parseShort(hashParts[0]);
            String subProductIdStr = hashParts[1];
            log.debug("---- [Callback] Extracted productId: {}, subProductId: {}", productId, subProductIdStr);
            log.info("---- [Callback] Extracted productId: {}, subProductId: {}", productId, subProductIdStr);

            Products product = productsRepository.findById(productId)
                    .orElseThrow(() -> new RuntimeException("Product not found with id: " + productId));
            SubProducts subProduct = subProductsRepository.findById(subProductIdStr)
                    .orElseThrow(() -> new RuntimeException("SubProduct not found with id: " + subProductIdStr));

            String baseTranxId = trnRefNo.startsWith("ORD") ? trnRefNo.substring(3) : trnRefNo;

            String txnId = commonMethods.buildTransactionId("TXN", baseTranxId);

            PgTransactionMapping mapping = pgTransactionMappingRepository
                    .findByOrderId(trnRefNo)
                    .orElseThrow(() ->
                            new RuntimeException("Record not found for orderId: " + trnRefNo)
                    );

            mapping.setTxnId(txnId);

            pgTransactionMappingRepository.save(mapping);

            // Build DTO (common for all statuses)
            TransactionSchemeDetailsRequestDto dto = new TransactionSchemeDetailsRequestDto();

            dto.setBaseTranxId(baseTranxId);
            dto.setAmount(txnAmt);
            dto.setConfirmAmount(txnAmt);
            dto.setOperatorName(request.getOtherInfo().getPgTrnType());
            dto.setOperatorCode(selectedScheme.getOperatorCode());
            dto.setSchemeRate(selectedScheme);
            dto.setTransactionType(product.getTransactionType());
            dto.setWalletType(subProduct.getWalletType());
            dto.setGstValue(String.valueOf(product.getSgst()));
            dto.setTdsRate(String.valueOf(product.getTds()));
            dto.setHierarchyId(pgTransactionMapping.getInitiatedBy());
            dto.setProductCode(product.getProductCode());
            dto.setSubProductCode(subProduct.getSubProductCode());
            dto.setIsCommission(product.getIsCommissions());
            dto.setHasCharge(product.getHasCharges());

            log.debug("---- [Callback] DTO prepared: {}", dto);
            log.info("---- [Callback] DTO prepared: {}", dto);

            // Forward to central transaction engine
            log.info("---- [Callback] Forwarding to Transaction Engine with Status: {}", vendorStatus);
            transactionServiceImpl.processTransaction(dto, vendorStatus);

            log.info("---- [Callback] Processing complete for Order ID: {} | Status: {}", trnRefNo, vendorStatus);

            // Call your internal transaction engine here
//            TransactionSchemeDetailsRequestDto transactionSchemeDetailsRequestDto = new TransactionSchemeDetailsRequestDto();
//            transactionServiceImpl.processTransaction(transactionSchemeDetailsRequestDto, vendorStatus);

        } catch (Exception e) {
            log.error("Async processing failed for orderId: {}", trnRefNo, e);
            // DO NOT THROW
        }
    }
}
