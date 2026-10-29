package com.scs.admin.service.impl;

import com.scs.admin.dto.*;
import com.scs.admin.entities.*;
import com.scs.admin.entities.product.Products;
import com.scs.admin.repositories.*;
import com.scs.admin.response.CommonResponse;
import com.scs.admin.response.RunPaisaCallback;
import com.scs.admin.service.PayoutBeneficiaryService;
import com.scs.admin.util.TokenUtil;
import jakarta.persistence.criteria.Predicate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.*;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

@Service
@Slf4j
public class PayoutBeneficiaryServiceImpl implements PayoutBeneficiaryService {

    @Autowired
    private BeneficiaryBankAccountRepository beneficiaryBankAccountRepository;

    @Autowired
    private SubProductsRepository subProductsRepository;

    @Autowired
    private TransactionEngineServiceImpl transactionEngineServiceImpl;

    @Autowired
    private SchemeRepository schemeRepository;

    @Autowired
    private SchemeRateRepository schemeRateRepository;

    @Autowired
    private ProductsRepository productsRepository;

    @Autowired
    private PayoutPendingTransactionRepository payoutPendingTransactionRepository;

    @Autowired
    private NetworkBankMasterRepository networkBankMasterRepository;

    @Autowired
    private TokenUtil tokenUtil;


    @Override
    public CommonResponse<BeneficiaryBankAccount> saveBeneficiary(BeneficiaryBankAccountDTO dto) {

        // Count existing records
        long count = beneficiaryBankAccountRepository.count();

        //Check limit (max 200)
        if (count >= 200) {
            return CommonResponse.handleErrors("Maximum limit of 200 beneficiary records reached. Cannot add more.");
        }

        // Fetch bank name from NetworkBankMaster
        Long bankId = Long.valueOf(dto.getBankName());

        NetworkBankMaster networkBank = networkBankMasterRepository
                .findById(bankId)
                .orElseThrow(() ->
                        new RuntimeException("Bank not found for id: " + bankId));

        // Map DTO to Entity
        BeneficiaryBankAccount entity = new BeneficiaryBankAccount();
        entity.setRetailerId(dto.getRetailerId());
        entity.setBankName(networkBank.getBankName());
        entity.setBankHolderName(dto.getBankHolderName());
        entity.setAccountNumber(dto.getAccountNumber());
        entity.setIfscCode(dto.getIfscCode());
        entity.setEmailId(dto.getEmailId());
        entity.setMobileNo(dto.getMobileNo());
        entity.setCreatedBy(dto.getCreatedBy());

        // Save entity
        BeneficiaryBankAccount saved = beneficiaryBankAccountRepository.save(entity);

        // Return success response
        return CommonResponse.success(saved, "Bank account saved successfully");
    }


    @Override
    public CommonResponse<String> deleteMultipleBeneficiaries(List<Long> ids) {
        List<Long> notFound = new ArrayList<>();

        for (Long id : ids) {
            if (beneficiaryBankAccountRepository.existsById(id)) {
                beneficiaryBankAccountRepository.deleteById(id);
            } else {
                notFound.add(id);
            }
        }

        String message = "Deleted successfully.";
        if (!notFound.isEmpty()) {
            message += " But some IDs not found: " + notFound;
        }

        return CommonResponse.success(message, "Processed IDs: " + ids);
    }

    @Override
    public CommonResponse<Page<BeneficiaryBankAccount>> getAllFiltered(
            Long id,
            String retailerId,
            String bankName,
            String bankHolderName,
            String accountNumber,
            String ifscCode,
            String paymentMode,
            Boolean isVerified,
            LocalDate createdOn,
            String createdBy,
            Pageable pageable) {

        String loginHierarchyId = tokenUtil.extractHierarchyId();

        Page<BeneficiaryBankAccount> results =
                beneficiaryBankAccountRepository.findAll((root, query, cb) -> {

                    List<Predicate> predicates = new ArrayList<>();

                    // IMPORTANT: logged-in hierarchy can see only own banks
                    predicates.add(cb.equal(root.get("retailerId"), loginHierarchyId));

                    if (id != null) {
                        predicates.add(cb.equal(root.get("id"), id));
                    }

                    if (retailerId != null && !retailerId.isBlank()) {
                        predicates.add(cb.like(
                                cb.lower(root.get("retailerId")),
                                "%" + retailerId.toLowerCase() + "%"
                        ));
                    }

                    if (bankName != null && !bankName.isBlank()) {
                        predicates.add(cb.like(
                                cb.lower(root.get("bankName")),
                                "%" + bankName.toLowerCase() + "%"
                        ));
                    }

                    if (bankHolderName != null && !bankHolderName.isBlank()) {
                        predicates.add(cb.like(
                                cb.lower(root.get("bankHolderName")),
                                "%" + bankHolderName.toLowerCase() + "%"
                        ));
                    }

                    if (accountNumber != null && !accountNumber.isBlank()) {
                        predicates.add(cb.like(
                                cb.lower(root.get("accountNumber")),
                                "%" + accountNumber.toLowerCase() + "%"
                        ));
                    }

                    if (ifscCode != null && !ifscCode.isBlank()) {
                        predicates.add(cb.like(
                                cb.lower(root.get("ifscCode")),
                                "%" + ifscCode.toLowerCase() + "%"
                        ));
                    }

                    if (paymentMode != null && !paymentMode.isBlank()) {
                        predicates.add(cb.like(
                                cb.lower(root.get("paymentMode")),
                                "%" + paymentMode.toLowerCase() + "%"
                        ));
                    }

                    if (isVerified != null) {
                        predicates.add(cb.equal(root.get("isVerified"), isVerified));
                    }

                    if (createdOn != null) {
                        predicates.add(cb.equal(
                                cb.function("DATE", LocalDate.class, root.get("createdOn")),
                                createdOn
                        ));
                    }

                    if (createdBy != null && !createdBy.isBlank()) {
                        predicates.add(cb.like(
                                cb.lower(root.get("createdBy")),
                                "%" + createdBy.toLowerCase() + "%"
                        ));
                    }

                    return cb.and(predicates.toArray(new Predicate[0]));
                }, pageable);

        return CommonResponse.success(results, "Filtered results");
    }

    @Override
    public CommonResponse<BeneficiaryBankAccountDTO> updateBeneficiary(Long id, BeneficiaryBankAccountDTO requestDto) {
        Optional<BeneficiaryBankAccount> optional = beneficiaryBankAccountRepository.findById(id);

        if (optional.isEmpty()) {
            return CommonResponse.error(HttpStatus.BAD_REQUEST.value(),
                    "Beneficiary Bank Account not found for ID: " + id);
        }
        
        BeneficiaryBankAccount entity = optional.get();

        // Update fields
        if (requestDto.getRetailerId() != null) entity.setRetailerId(requestDto.getRetailerId());
        if (requestDto.getBankName() != null) entity.setBankName(requestDto.getBankName());
        if (requestDto.getBankHolderName() != null) entity.setBankHolderName(requestDto.getBankHolderName());
        if (requestDto.getAccountNumber() != null) entity.setAccountNumber(requestDto.getAccountNumber());
        if (requestDto.getIfscCode() != null) entity.setIfscCode(requestDto.getIfscCode());
        if (requestDto.getPaymentMode() != null) entity.setPaymentMode(requestDto.getPaymentMode());
        if (requestDto.getIsVerified() != null) entity.setIsVerified(requestDto.getIsVerified());
        if (requestDto.getEmailId() != null) entity.setEmailId(requestDto.getEmailId());
        if (requestDto.getMobileNo() != null) entity.setMobileNo(requestDto.getMobileNo());
        if (requestDto.getCreatedBy() != null) entity.setCreatedBy(requestDto.getCreatedBy());

        beneficiaryBankAccountRepository.save(entity);

        // Convert updated entity to DTO
        BeneficiaryBankAccountDTO responseDto = new BeneficiaryBankAccountDTO(
                entity.getId(),
                entity.getRetailerId(),
                entity.getBankName(),
                entity.getBankHolderName(),
                entity.getAccountNumber(),
                entity.getIfscCode(),
                entity.getPaymentMode(),
                entity.getIsVerified(),
                entity.getEmailId(),
                entity.getMobileNo(),
                entity.getCreatedBy()
        );

        return CommonResponse.success(responseDto, "Beneficiary Bank Account updated successfully");
    }

    public void processCallback(RunPaisaCallback callback) {
        log.info("========== [RunPaisa CALLBACK RECEIVED] ==========");
        log.info("========== ORDER_ID: {}, STATUS: {}, TXN_AMOUNT: {}",
                callback.getOrderId(), callback.getStatus(), callback.getTxnAmount());

        TransactionSchemeDetailsRequestDto dto = null;

        try {
            // Fetch pending transaction (for all statuses)
            log.info("[1] Fetching pending transaction for ORDER_ID: {}", callback.getOrderId());
            PayoutPendingTransactions pendingTxn = payoutPendingTransactionRepository.findByTransactionId(callback.getOrderId())
                    .orElseThrow(() -> new RuntimeException("Pending transaction not found for order ID: " + callback.getOrderId()));

            log.info("[1.1] PendingTxn fetched successfully: ID={}, SchemeCode={}, TxnType={}, Amount={}, Status={}",
                    pendingTxn.getId(), pendingTxn.getSchemeCode(), pendingTxn.getTransactionType(),
                    pendingTxn.getAmount(), pendingTxn.getStatus());

            Double txnAmt = Double.valueOf(callback.getTxnAmount());

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

            String orderId = callback.getOrderId();
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
            transactionEngineServiceImpl.processTransaction(dto, callback.getStatus());
            log.info("[7] Transaction processed successfully for ORDER_ID: {}, STATUS={}",
                    callback.getOrderId(), callback.getStatus());

        } catch (Exception ex) {
            log.error("[ERROR] Exception while processing callback for ORDER_ID={} | Error={}",
                    callback.getOrderId(), ex.getMessage(), ex);
        }

        log.info("========== [RunPaisa CALLBACK PROCESSING COMPLETED] ORDER_ID={} ==========", callback.getOrderId());
    }

}
