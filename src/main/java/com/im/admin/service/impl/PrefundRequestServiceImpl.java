package com.im.admin.service.impl;


import com.im.admin.dto.AbstractWalletAmountTable;
import com.im.admin.dto.FundRequestActionDto;
import com.im.admin.dto.PrefundRequestDto;
import com.im.admin.dto.TransactionSchemeDetailsRequestDto;
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
    public CommonResponse<PrefundRequest> saveWalletLoadRequest(PrefundRequestDto dto, MultipartFile imageFile) throws IOException {

        double depositAmount = Double.parseDouble(dto.getDepositAmount());
        double confirmAmount = Double.parseDouble(dto.getConfirmAmount());

        if (depositAmount != confirmAmount) {
            throw new IllegalArgumentException("Deposit amount and confirmation amount do not match.");
        }

        String fileExtension = imageFile.getOriginalFilename();
        if (fileExtension == null || !fileExtension.matches("(?i).*\\.(jpg|jpeg|png|pdf)$")) {
            throw new IllegalArgumentException("Invalid image format. Only JPG, JPEG, or PNG are allowed.");
        }

        Optional<PrefundRequest> approved = repository.findApprovedByBankTranId(dto.getBankTranId());
        if (approved.isPresent()) {
            throw new IllegalArgumentException("This transaction has already been approved and cannot be re-submitted.");
        }

        LocalDateTime fiveMinutesAgo = LocalDateTime.now().minusMinutes(5);
        List<PrefundRequest> duplicates = repository.findDuplicatesWithinTime(dto.getBankTranId(), depositAmount, fiveMinutesAgo);
        if (!duplicates.isEmpty()) {
            throw new IllegalArgumentException("Duplicate request detected within last 5 minutes.");
        }

        String fileName = imageUtil.saveImage(imageFile, "Prefund");

        // Save new record
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
        entity.setIpAddress(dto.getIpAddress());
        if (dto.getDepositDate() != null && !dto.getDepositDate().isEmpty()) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            entity.setDepositDate(LocalDate.parse(dto.getDepositDate()));
        }
        entity.setDepositType(dto.getDepositType());
        entity.setRemarks(dto.getRemarks());

        PrefundRequest saved = repository.save(entity);

        return new CommonResponse<>(200, "Prefund request submitted successfully : " + dto.getConfirmAmount(), saved);
    }

    @Override
    public Page<PrefundRequest> getAllPrefundRequests(int page, int size, Long id, String requestedBy, String mobileNumber, Double depositAmount, String status, LocalDate depositDate, LocalDate fromDate,
                                                      LocalDate toDate, LocalDate createDateTime,
                                                      String bankHolderName, String bankAccountName, String bankAccountNumber,
                                                      String bankTranId, String paymentMode, String narration, String ipAddress,
                                                      LocalDate approveOrRejectDate, String approveOrRejectTime, String remarks, String depositType) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createDateTime"));
        Specification<PrefundRequest> spec = PrefundRequestSpecification.getFilter(
                id, requestedBy, mobileNumber, depositAmount, status, depositDate,
                fromDate, toDate,
                createDateTime, bankHolderName, bankAccountName, bankAccountNumber,
                bankTranId, paymentMode, narration, ipAddress,
                approveOrRejectDate, approveOrRejectTime, remarks, depositType
        );

        return repository.findAll(spec, pageable);
    }

    @Transactional
    @Override
    public CommonResponse<String> takeActionOnFundRequest(FundRequestActionDto actionDto, Double latitude, Double longitude) {

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

        if (newStatus == Status.APPROVED) {
            String requesterId = fundRequest.getRequestedBy();
            String approverId = actionDto.getApproveOrRejectHierarchyId();
            WalletType walletType = WalletType.PREPAID;
            BigDecimal amount = BigDecimal.valueOf(fundRequest.getDepositAmount());

            AbstractWalletAmountTable approverWallet = commonMethods.getWalletByType(walletType.name(), approverId);
            BigDecimal approverBalance = approverWallet.getAvailableBalance();

            if (approverBalance.compareTo(amount) < 0) {
                return new CommonResponse<>(400, "Insufficient balance in approver's wallet: " + approverId, null);
            }

            updateWallet(approverWallet, approverId, walletType, amount, "Debit", baseTransactionId, "SUCCESS", "DEBIT", "Fund Transfer Successfully to" + requesterId + "/ @" + date + " " + time);

            String requesterType = commonMethods.extractHierarchyType(requesterId);
            String parentHierarchyId = requesterId;

            if ("RU".equalsIgnoreCase(requesterType)) {
                log.debug("Hierarchy type is RU. Fetching parent Retailer hierarchyId...");

                parentHierarchyId = getParentRetailerId(requesterId);
                log.debug("Resolved parent Retailer hierarchyId: {}", parentHierarchyId);
            }

            AbstractWalletAmountTable requesterWallet = commonMethods.getWalletByType(walletType.name(), parentHierarchyId);
            updateWallet(requesterWallet, parentHierarchyId, walletType, amount, "Credit", baseTransactionId, "SUCCESS", "CREDIT", "Fund Transfer Successfully from " + approverId + "/ @" + date + " " + time);
        }

        repository.save(fundRequest);

        SubProducts subProduct = subProductsRepository
                .findBySubProductCode(actionDto.getSubProductId())
                .orElseThrow(() -> new RuntimeException("SubProduct not found for Bank Verification"));

        // Get Product from SubProduct
        Products product = subProduct.getProduct();

        log.info("---- [Callback] Product fetched successfully | productId={}", product.getId());

        // ================== NEW LOGIC START ==================

        String requesterId = fundRequest.getRequestedBy();
        String requesterType = commonMethods.extractHierarchyType(requesterId);

        String schemeCode;

        if ("RT".equalsIgnoreCase(requesterType)) {
            // For RT → take from request
            schemeCode = actionDto.getSchemeCode();
        } else {

            // Handle RU → take parent
            String hierarchyForScheme = requesterId;
            if ("RU".equalsIgnoreCase(requesterType)) {
                hierarchyForScheme = getParentRetailerId(requesterId);
            }

            // For SD/PT/DT → derive
            String schemeName = getSchemeNameByHierarchy(hierarchyForScheme);

            Optional<Scheme> scheme = Optional.ofNullable(schemeRepository.findBySchemeName(schemeName)
                    .orElseThrow(() -> new RuntimeException("Scheme not found for name: " + schemeName)));

            schemeCode = scheme.get().getSchemeCode();
        }

        // ================== NEW LOGIC END ==================

        Optional<SchemeRate> optSchemeRate = schemeRateRepository
                .findSchemeRateByCodeAndOperatorAndAmount(
                        schemeCode,
                        fundRequest.getBankAccountName(),
                        fundRequest.getDepositAmount()
                );

        SchemeRate schemeRate = optSchemeRate.orElseThrow(
                () -> new RuntimeException("SchemeRate not found for code: " + schemeCode)
        );

        BigDecimal grossAmount = BigDecimal.valueOf(schemeRate.getGross());

        log.info("Scheme rate fetched successfully | schemeCode={} | rateType={} | slabType={}",
                schemeRate.getSchemeCode(), schemeRate.getRateType(), schemeRate.getSlabType());

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

                TransactionSchemeDetailsRequestDto dto = walletTransactionService.buildTransactionDto(
                        grossAmount,
                        baseTransactionId,
                        schemeRate,
                        product,
                        subProduct,
                        fundRequest.getRequestedBy(),
                        walletType
                );

                transactionEngineServiceImpl.processCommission(dto, baseTransactionId, "SUCCESS", dto);
            }

        } catch (Exception e) {
            log.error("**** [processChargesAndCommission] ERROR during processing {} {} ****",
                    product.getHasCharges(), product.getIsCommissions(), e);
            throw e;
        }

        return new CommonResponse<>(200, "Fund request action processed successfully", null);
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
    }

    @Override
    public CommonResponse<PrefundRequest> getById(Long id) {
        Optional<PrefundRequest> optional = repository.findById(id);
        return optional.map(prefundRequest ->
                new CommonResponse<>(200, "Prefund request fetched successfully", prefundRequest))
                .orElseGet(() ->
                        new CommonResponse<>(404, "Prefund request not found with ID: " + id, null));
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
