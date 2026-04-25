package com.im.admin.service.impl;


import com.im.admin.dto.*;
import com.im.admin.entities.*;
import com.im.admin.entities.enums.Status;
import com.im.admin.entities.enums.WalletType;
import com.im.admin.entities.product.Products;
import com.im.admin.entities.vendor.Vendors;
import com.im.admin.repositories.*;
import com.im.admin.response.CommonResponse;
import com.im.admin.service.PrefundRequestService;
import com.im.admin.spec.PrefundRequestSpecification;
import com.im.admin.util.CommonMethods;
import com.im.admin.util.ImageUtil;
import com.im.admin.util.WalletTransactionService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
@Validated
@Slf4j
public class PrefundRequestServiceImpl implements PrefundRequestService {

    @Autowired
    private PrefundRequestRepository repository;

    @Autowired
    private CommonMethods commonMethods;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private ImageUtil imageUtil;

    @Autowired
    private RetailerUserRepository retailerUserRepository;

    @Autowired
    private SubProductsRepository subProductsRepository;

    @Autowired
    private VendorRoutingRepository vendorRoutingRepository;

    @Autowired
    private VendorCredentialsRepository vendorCredentialsRepository;

    @Autowired
    private WalletTransactionService walletTransactionService;

    @Autowired
    private SchemeRateRepository schemeRateRepository;

    @Autowired
    private SchemeRepository schemeRepository;

    @Autowired
    private TransactionEngineServiceImpl transactionEngineServiceImpl;

    @Override
    public CommonResponse<PrefundRequest> saveWalletLoadRequest(PrefundRequestDto dto, MultipartFile imageFile, String ipAddress) throws IOException {

        double depositAmount = Double.parseDouble(dto.getDepositAmount());
        double confirmAmount = Double.parseDouble(dto.getConfirmAmount());

        if (depositAmount != confirmAmount) {
            throw new IllegalArgumentException("Deposit amount and confirmation amount do not match.");
        }

        String fileExtension = imageFile.getOriginalFilename();
        if (fileExtension == null || !fileExtension.matches("(?i).*\\.(jpg|jpeg|png|pdf)$")) {
            throw new IllegalArgumentException("Invalid image format. Only JPG, JPEG, PNG, PDF allowed.");
        }

        Optional<PrefundRequest> approved = repository.findApprovedByBankTranId(dto.getBankTranId());
        if (approved.isPresent()) {
            throw new IllegalArgumentException("This transaction already approved.");
        }

        LocalDateTime fiveMinutesAgo = LocalDateTime.now().minusMinutes(5);
        List<PrefundRequest> duplicates =
                repository.findDuplicatesWithinTime(dto.getBankTranId(), depositAmount, fiveMinutesAgo);

        if (!duplicates.isEmpty()) {
            throw new IllegalArgumentException("Duplicate request detected within last 5 minutes.");
        }

        String fileName = imageUtil.saveImage(imageFile, "Prefund");

        PrefundRequest entity = new PrefundRequest();
        entity.setRequestedBy(dto.getRequestedBy());
        entity.setMobileNumber(dto.getMobileNumber());
        entity.setDepositAmount(depositAmount);
        entity.setDepositImage(fileName);
        entity.setBankAccountName(dto.getBankAccountName());
        entity.setBankHolderName(dto.getBankHolderName());
        entity.setBankAccountNumber(dto.getBankAccountNumber());
        entity.setBankTranId(dto.getBankTranId());
        entity.setPaymentMode(dto.getPaymentMode());
        entity.setNarration(dto.getNarration());
        entity.setIpAddress(ipAddress);

        if (dto.getDepositDate() != null && !dto.getDepositDate().isEmpty()) {
            entity.setDepositDate(LocalDate.parse(dto.getDepositDate()));
        }

        entity.setDepositType(dto.getDepositType());
        entity.setRemarks(dto.getRemarks());

        String requesterId = dto.getRequestedBy();
        String requesterType = commonMethods.extractHierarchyType(requesterId);

        SubProducts subProduct;
        Scheme scheme;

        if ("RT".equalsIgnoreCase(requesterType) || "RU".equalsIgnoreCase(requesterType)) {

            // From request
            subProduct = subProductsRepository
                    .findById(dto.getSubProductId())
                    .orElseThrow(() -> new RuntimeException("SubProduct not found"));

            scheme = schemeRepository.findBySchemeCode(dto.getSchemeCode())
                    .orElseThrow(() -> new RuntimeException("Scheme not found"));

        } else {

            // From scheme mapping (DT/PT/SD)
            String schemeName = getSchemeNameByHierarchy(requesterId);

            scheme = schemeRepository.findBySchemeName(schemeName)
                    .orElseThrow(() -> new RuntimeException("Scheme not found"));

            String[] hashParts = scheme.getSchemeCombinationHashCode().split("-");
            String subProductId = hashParts[1];

            subProduct = subProductsRepository.findById(subProductId)
                    .orElseThrow(() -> new RuntimeException("SubProduct not found"));
        }

        entity.setSubProducts(subProduct);
        entity.setScheme(scheme);

        PrefundRequest saved = repository.save(entity);

        return new CommonResponse<>(200,
                "Prefund request submitted successfully : " + dto.getConfirmAmount(), saved);
    }

    @Override
    public Page<PrefundRequestResponseDTO> getAllPrefundRequests(
            int page, int size, Long id, String requestedBy, String mobileNumber,
            Double depositAmount, String status, LocalDate depositDate,
            LocalDate fromDate, LocalDate toDate, LocalDate createDateTime,
            String bankHolderName, String bankAccountName, String bankAccountNumber,
            String bankTranId, String paymentMode, String narration, String ipAddress,
            LocalDate approveOrRejectDate, String approveOrRejectTime,
            String remarks, String depositType) {

        Pageable pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.DESC, "createDateTime"));

        Specification<PrefundRequest> spec = PrefundRequestSpecification.getFilter(
                id, requestedBy, mobileNumber, depositAmount, status, depositDate,
                fromDate, toDate, createDateTime, bankHolderName, bankAccountName,
                bankAccountNumber, bankTranId, paymentMode, narration, ipAddress,
                approveOrRejectDate, approveOrRejectTime, remarks, depositType
        );

        Page<PrefundRequest> pageResult = repository.findAll(spec, pageable);

        return pageResult.map(p -> PrefundRequestResponseDTO.builder()
                .id(p.getId())
                .requestedBy(p.getRequestedBy())
                .mobileNumber(p.getMobileNumber())
                .depositAmount(p.getDepositAmount())
                .status(p.getStatus())
                .depositDate(p.getDepositDate())
                .createDateTime(p.getCreateDateTime())
                .bankHolderName(p.getBankHolderName())
                .bankAccountName(p.getBankAccountName())
                .bankAccountNumber(p.getBankAccountNumber())
                .bankTranId(p.getBankTranId())
                .narration(p.getNarration())
                .paymentMode(p.getPaymentMode())
                .ipAddress(p.getIpAddress())
                .approveOrRejectDate(p.getApproveOrRejectDate())
                .approveOrRejectTime(p.getApproveOrRejectTime())
                .remarks(p.getRemarks())
                .depositType(p.getDepositType())

                .subProductName(p.getSubProducts() != null ? p.getSubProducts().getSubProductTitle() : null)
                .productName(p.getSubProducts() != null && p.getSubProducts().getProduct() != null
                        ? p.getSubProducts().getProduct().getName() : null)
                .schemeName(p.getScheme() != null ? p.getScheme().getSchemeName() : null)

                .build()
        );
    }

    @Transactional
    @Override
    public CommonResponse<String> takeActionOnFundRequest(FundRequestActionDto actionDto) {

        String baseTransactionId = commonMethods.generateBaseTransactionId();
        LocalDate date = LocalDate.now();
        LocalTime time = LocalTime.now();

        PrefundRequest fundRequest = repository.findById(actionDto.getFundRequestId())
                .orElseThrow(() -> new RuntimeException("Fund Request not found"));

        if (fundRequest.getStatus() != Status.PENDING) {
            return new CommonResponse<>(400, "Fund Request already processed", null);
        }

        Status newStatus = actionDto.getStatus();
        fundRequest.setStatus(newStatus);
        fundRequest.setRemarks(actionDto.getRemarks());
        fundRequest.setApproveOrRejectDate(date);
        fundRequest.setApproveOrRejectTime(time);

        // ================= WALLET FLOW =================
        if (newStatus == Status.APPROVED) {

            String requesterId = fundRequest.getRequestedBy();
            String approverId = actionDto.getApproveOrRejectHierarchyId();
            WalletType walletType = WalletType.PREPAID;
            BigDecimal amount = BigDecimal.valueOf(fundRequest.getDepositAmount());

            AbstractWalletAmountTable approverWallet =
                    commonMethods.getWalletByType(walletType.name(), approverId);

            if (approverWallet.getAvailableBalance().compareTo(amount) < 0) {
                throw new RuntimeException("Insufficient balance in approver's wallet");

            }

            updateWallet(approverWallet, approverId, walletType, amount,
                    "Debit", baseTransactionId, "SUCCESS", "DEBIT",
                    "Fund Transfer to " + requesterId);

            String requesterType = commonMethods.extractHierarchyType(requesterId);
            String parentHierarchyId = requesterId;

            if ("RU".equalsIgnoreCase(requesterType)) {
                parentHierarchyId = getParentRetailerId(requesterId);
            }

            AbstractWalletAmountTable requesterWallet =
                    commonMethods.getWalletByType(walletType.name(), parentHierarchyId);

            updateWallet(requesterWallet, parentHierarchyId, walletType, amount,
                    "Credit", baseTransactionId, "SUCCESS", "CREDIT",
                    "Fund Transfer from " + approverId);
        }

        repository.save(fundRequest);

        // ================= UNIFIED RESOLUTION =================

        SubProducts subProduct = fundRequest.getSubProducts();
        Scheme scheme = fundRequest.getScheme();
        Products product = subProduct.getProduct();

        String schemeCode = scheme.getSchemeCode();

        log.info("Resolved from DB | productId={}, subProductId={}, schemeCode={}",
                product.getId(), subProduct.getId(), schemeCode);

        // ================= SCHEME RATE =================

        SchemeRate schemeRate = schemeRateRepository
                .findSchemeRateByCodeAndOperatorAndAmount(
                        schemeCode,
                        fundRequest.getBankAccountName(),
                        fundRequest.getDepositAmount()
                )
                .orElseThrow(() -> new RuntimeException("SchemeRate not found"));

        BigDecimal grossAmount = BigDecimal.valueOf(schemeRate.getGross());

        try {

            if (Boolean.TRUE.equals(product.getHasCharges())) {
                walletTransactionService.processHierarchyCharges(
                        fundRequest.getRequestedBy(),
                        product,
                        subProduct,
                        schemeRate,
                        grossAmount,
                        baseTransactionId
                );
            }

            if (Boolean.TRUE.equals(product.getIsCommissions())) {

                WalletType walletType = "POSTPAID".equalsIgnoreCase(subProduct.getWalletType())
                        ? WalletType.POSTPAID
                        : WalletType.PREPAID;

                TransactionSchemeDetailsRequestDto dto =
                        walletTransactionService.buildTransactionDto(
                                grossAmount,
                                baseTransactionId,
                                schemeRate,
                                product,
                                subProduct,
                                fundRequest.getRequestedBy(),
                                walletType
                        );

                transactionEngineServiceImpl.processCommission(
                        dto, baseTransactionId, "SUCCESS", dto);
            }

        } catch (Exception e) {
            log.error("Error in charges/commission", e);
            throw e;
        }

        return new CommonResponse<>(200,
                "Fund request action processed successfully", null);
    }

    public String getParentRetailerId(String retailerUserHierarchyId) {
        return retailerUserRepository.findById(retailerUserHierarchyId)
                .map(RetailerUser::getParentAccount)
                .orElseThrow(() -> new RuntimeException("Parent Retailer not found for Retailer User: " + retailerUserHierarchyId));
    }

    private <T extends AbstractWalletAmountTable> void updateWallet(T wallet, String hierarchyId, WalletType walletType, BigDecimal amount,
                                                                    String tranxType, String baseTransactionId, String tranStatus, String service, String remarks) {

        String actionOnBalance;

        // *** Credit or Debit Operation ***
        BigDecimal balanceBefore = wallet.getAvailableBalance();

        if (tranxType.equalsIgnoreCase("credit")) {
            wallet.setTotalCash(wallet.getTotalCash().add(amount));
            wallet.setAvailableBalance(wallet.getAvailableBalance().add(amount));
            actionOnBalance = "Add Funds - " + walletType.name();
        } else {
            wallet.setTotalCash(wallet.getTotalCash().subtract(amount));
            wallet.setAvailableBalance(wallet.getAvailableBalance().subtract(amount));
            actionOnBalance = "Deduct Funds - " + walletType.name();
        }

        wallet.setLastUpdatedAmount(amount);
        wallet.setLastUpdatedDate(LocalDate.now());
        wallet.setLastUpdatedTime(LocalTime.now());

        // Insert transaction
        insertTransaction(walletType.name(), hierarchyId, tranStatus, amount, balanceBefore,
                wallet.getAvailableBalance(), actionOnBalance, "TXN" + baseTransactionId, service, remarks);

    }

    private void insertTransaction(String walletType, String hierarchyId, String tranStatus, BigDecimal amount, BigDecimal beforeBalance,
                                   BigDecimal afterBalance, String actionOnBalanceAmount, String tranxId, String service, String remarks) {

        String hierarchyType = commonMethods.extractHierarchyType(hierarchyId);
        String transactionTableName = commonMethods.getTransactionTable(hierarchyType, walletType);
        String columnName = commonMethods.getHierarchyColumnName(hierarchyType);

        log.info("Inserting into {} | Type: {} | Amount: {} | Hierarchy ID: {} | Before Balance: {} | After Balance: {}",
                transactionTableName, service, amount, hierarchyId, beforeBalance, afterBalance);

        String sql = "INSERT INTO " + transactionTableName +
                " (" + columnName + "  ,service, amount, date_of_transaction, time_of_transaction, " +
                "bal_before_tran, bal_after_tran, action_on_balance_amount, final_balance, transaction_id, tran_status, remarks) " +
                "VALUES (:hierarchyId, :service, :amount, :dateOfTransaction, :timeOfTransaction, " +
                ":beforeTransactionBalance, :afterTransactionBalance, :actionOnBalanceAmount, :finalBalance, :tranxId, :tranStatus, :remarks)";

        try {
            entityManager.createNativeQuery(sql)
                    .setParameter("hierarchyId", hierarchyId)
                    .setParameter("service", service)
                    .setParameter("amount", amount)
                    .setParameter("dateOfTransaction", new Date())
                    .setParameter("timeOfTransaction", new Time(System.currentTimeMillis()))
                    .setParameter("beforeTransactionBalance", beforeBalance)
                    .setParameter("afterTransactionBalance", afterBalance)
                    .setParameter("actionOnBalanceAmount", actionOnBalanceAmount)
                    .setParameter("finalBalance", afterBalance)
                    .setParameter("tranxId", tranxId)
                    .setParameter("tranStatus", tranStatus)
                    .setParameter("remarks", remarks)
                    .executeUpdate();

        } catch (PersistenceException ex) {

            // Handle duplicate safely
            if (ex.getMessage() != null &&
                    (ex.getMessage().contains("constraint") || ex.getMessage().contains("Duplicate"))) {

                log.warn("Duplicate transaction skipped | TranxID={}", tranxId);
                return; // skip insert, continue flow
            }

            log.error("Failed to insert transaction | TranxID={}", tranxId, ex);
            throw ex; // real error → propagate
        }

    }

    @Override
    public CommonResponse<PrefundRequestResponseDTO> getById(Long id) {

        Optional<PrefundRequest> optional = repository.findById(id);

        return optional.map(entity -> {

            PrefundRequestResponseDTO dto = PrefundRequestResponseDTO.builder()
                    .id(entity.getId())
                    .requestedBy(entity.getRequestedBy())
                    .mobileNumber(entity.getMobileNumber())
                    .depositAmount(entity.getDepositAmount())
                    .status(entity.getStatus())

                    .depositDate(entity.getDepositDate())
                    .createDateTime(entity.getCreateDateTime())

                    .bankHolderName(entity.getBankHolderName())
                    .bankAccountName(entity.getBankAccountName())
                    .bankAccountNumber(entity.getBankAccountNumber())
                    .bankTranId(entity.getBankTranId())

                    .narration(entity.getNarration())
                    .paymentMode(entity.getPaymentMode())
                    .ipAddress(entity.getIpAddress())

                    .approveOrRejectDate(entity.getApproveOrRejectDate())
                    .approveOrRejectTime(entity.getApproveOrRejectTime())

                    .remarks(entity.getRemarks())
                    .depositType(entity.getDepositType())

                    .subProductName(entity.getSubProducts() != null ? entity.getSubProducts().getSubProductTitle() : null)
                    .productName(entity.getSubProducts() != null && entity.getSubProducts().getProduct() != null
                            ? entity.getSubProducts().getProduct().getName() : null)
                    .schemeName(entity.getScheme() != null ? entity.getScheme().getSchemeName() : null)

                    .build();

            return new CommonResponse<>(200, "Prefund request fetched successfully", dto);

        }).orElseGet(() ->
                new CommonResponse<>(404, "Prefund request not found with ID: " + id, null)
        );
    }

    public String getSchemeNameByHierarchy(String hierarchyId) {

        String type = walletTransactionService.extractHierarchyType(hierarchyId);

        return switch (type.toUpperCase()) {
            case "SD" -> "Fund_Request_SD_Scheme";
            case "PT" -> "Fund_Request_PT_Scheme";
            case "DT" -> "Fund_Request_DT_Scheme";
            default -> throw new RuntimeException("No scheme defined for hierarchy: " + type);
        };
    }

}
