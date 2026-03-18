package com.im.admin.service;

import com.im.admin.commons.CommonFunctions;
import com.im.admin.dto.TransactionSchemeDetailsRequestDto;
import com.im.admin.entities.*;
import com.im.admin.entities.product.Products;
import com.im.admin.repositories.*;
import com.im.admin.service.impl.TransactionEngineServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Component
public class PayoutCheckStatusSchedulerService {

    @Autowired
    private PayoutPendingTransactionRepository payoutPendingTransactionRepository;

    @Autowired
    private SchemeRepository schemeRepository;

    @Autowired
    private SchemeRateRepository schemeRateRepository;

    @Autowired
    private SubProductsRepository subProductsRepository;

    @Autowired
    private ProductsRepository productsRepository;

    @Autowired
    private TransactionEngineServiceImpl transactionEngineServiceImpl;

    RestTemplate restTemplate = new RestTemplate();

    public void checkStatusForPendingTransactions() {

        TransactionSchemeDetailsRequestDto dto = null;

        List<PayoutPendingTransactions> pendingList = payoutPendingTransactionRepository.findByStatus("PENDING");
        log.info("Checking status for {} pending transactions", pendingList.size());

        for (PayoutPendingTransactions txn : pendingList) {
            try {

                //call here
                log.info("[1] Fetching pending transaction for ORDER_ID: {}", txn.getTransactionId());
                PayoutPendingTransactions pendingTxn = payoutPendingTransactionRepository.findByTransactionId(txn.getTransactionId())
                        .orElseThrow(() -> new RuntimeException("Pending transaction not found for order ID: " + txn.getTransactionId()));

                log.info("[1.1] PendingTxn fetched successfully: ID={}, SchemeCode={}, TxnType={}, Amount={}, Status={}",
                        pendingTxn.getId(), pendingTxn.getSchemeCode(), pendingTxn.getTransactionType(),
                        pendingTxn.getAmount(), pendingTxn.getStatus());

                double txnAmt = txn.getAmount().doubleValue();

                // Fetch matching SchemeRate
                log.info("[2] Searching SchemeRate for schemeCode={}, operatorName={}, amount={}",
                        pendingTxn.getSchemeCode(), pendingTxn.getTransactionType(), txnAmt);

                SchemeRate selectedScheme = schemeRateRepository
                        .findSchemeRateByCodeAndOperatorAndAmount(
                                pendingTxn.getSchemeCode(),
                                pendingTxn.getTransactionType(),
                                txnAmt)
                        .orElseThrow(() -> new RuntimeException("No matching scheme rate found for amount: " + txnAmt));

                log.info("[2.1] Found SchemeRate: ID={}, RateType={}, SlabType={}, Gross={}, RetailerRate={}",
                        selectedScheme.getId(), selectedScheme.getRateType(), selectedScheme.getSlabType(),
                        selectedScheme.getGross(), selectedScheme.getRetailer());

                // Fetch scheme entity
                log.info("[3] Fetching Scheme entity for SchemeCode={}", selectedScheme.getSchemeCode());
                Scheme scheme = schemeRepository.findBySchemeCode(selectedScheme.getSchemeCode())
                        .orElseThrow(() -> new RuntimeException("Scheme Not Found with code: " + selectedScheme.getSchemeCode()));

                log.info("[3.1] Scheme fetched successfully: HashCode={}", scheme.getSchemeCombinationHashCode());

                // Parse scheme combination hash code
                String[] hashParts = scheme.getSchemeCombinationHashCode().split("-");
                if (hashParts.length < 2) {
                    throw new RuntimeException("Invalid schemeCombinationHashCode format: " + scheme.getSchemeCombinationHashCode());
                }

                short productId = Short.parseShort(hashParts[0]);
                String subProductIdStr = hashParts[1];
                String vendorId = hashParts[2];

                log.info("[4] Extracted ProductID={}, SubProductID={} from SchemeCombinationHashCode", productId, subProductIdStr);

                // Fetch product and subproduct
                Products product = productsRepository.findById(productId)
                        .orElseThrow(() -> new RuntimeException("Product not found with id: " + productId));
                SubProducts subProduct = subProductsRepository.findById(subProductIdStr)
                        .orElseThrow(() -> new RuntimeException("SubProduct not found with id: " + subProductIdStr));

                log.info("[4.1] Product/SubProduct fetched successfully: Product={}, SubProduct={}",
                        product.getName(), subProduct.getSubProductTitle());

                // Prepare DTO (common for all statuses)
                log.info("[5] Constructing TransactionSchemeDetailsRequestDto...");
                dto = new TransactionSchemeDetailsRequestDto();

                String orderId = txn.getTransactionId();
                String baseTxnId = orderId.startsWith("TXN") ? orderId.substring(3) : orderId;

                dto.setBaseTranxId(baseTxnId);
                dto.setAmount(txnAmt);
                dto.setConfirmAmount(txnAmt);
                dto.setOperatorName(pendingTxn.getTransactionType());
                dto.setOperatorCode(selectedScheme.getOperatorCode());
                dto.setSchemeRate(selectedScheme);
                dto.setTransactionType(product.getTransactionType());
                dto.setWalletType(subProduct.getWalletType());
                dto.setGstValue(String.valueOf(product.getSgst()));
                dto.setTdsRate(String.valueOf(product.getTds()));
                dto.setProductCode(product.getProductCode());
                dto.setSubProductCode(subProduct.getSubProductCode());
                dto.setIsCommission(product.getIsCommissions());
                dto.setHasCharge(product.getHasCharges());
                dto.setHierarchyId(pendingTxn.getRetailerId());

                log.info("[6] DTO ready for transaction processing: {}", dto);

                // Process transaction for any status (SUCCESS, FAILED, PENDING, etc.)
                transactionEngineServiceImpl.processTransaction(dto, txn.getStatus());
                log.info("[7] Transaction processed successfully for ORDER_ID: {}, STATUS={}", txn.getTransactionId(), txn.getStatus());

            } catch (Exception ex) {
                log.error("[ERROR] Exception while processing callback for ORDER_ID={} | Error={}",
                        txn.getTransactionId(), ex.getMessage(), ex);
            }
        }
    }

}
