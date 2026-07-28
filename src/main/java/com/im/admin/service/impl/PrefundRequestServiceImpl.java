package com.im.admin.service.impl;


import com.im.admin.dto.*;
import com.im.admin.entities.*;
import com.im.admin.entities.enums.Status;
import com.im.admin.entities.enums.WalletType;
import com.im.admin.entities.product.Products;
import com.im.admin.properties.FileUrlUtil;
import com.im.admin.repositories.*;
import com.im.admin.response.CommonResponse;
import com.im.admin.service.PrefundRequestService;
import com.im.admin.spec.PrefundRequestSpecification;
import com.im.admin.util.CommonMethods;
import com.im.admin.util.ImageUtil;
import com.im.admin.util.TokenUtil;
import com.im.admin.util.WalletTransactionService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
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
import java.util.*;

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

    @Autowired
    private ImBankMasterRepository imBankMasterRepository;

    @Autowired
    private TokenUtil tokenUtil;

    @Autowired
    private FileUrlUtil fileUrlUtil;

    @Autowired
    private AccessHierarchyNodeRepository accessHierarchyNodeRepository;

    @Autowired
    private SubProductsServiceImpl subProductsServiceImpl;

    @Override
    public CommonResponse<PrefundRequest> saveWalletLoadRequest(PrefundRequestDto dto, MultipartFile imageFile, RequestHeaderMetaDto requestHeaderMetaDtoMeta) throws IOException {

        double depositAmount = Double.parseDouble(dto.getDepositAmount());
        double confirmAmount = Double.parseDouble(dto.getConfirmAmount());

        if (depositAmount != confirmAmount) {
            throw new IllegalArgumentException("Deposit amount and confirmation amount do not match.");
        }

        String fileExtension = imageFile.getOriginalFilename();
        if (fileExtension == null || !fileExtension.matches("(?i).*\\.(jpg|jpeg|png|pdf)$")) {
            throw new IllegalArgumentException("Invalid image format. Only JPG, JPEG, PNG, PDF allowed.");
        }

        String bankName = imBankMasterRepository.findByAccountNumber(dto.getBankTransferTo())
                .map(ImBankMaster::getBankName)
                .orElseThrow(() -> new RuntimeException("Bank not found"));

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
        entity.setBankTransferTo(bankName);
        entity.setBankAccountName(dto.getBankAccountName());
        entity.setBankHolderName(dto.getBankHolderName());
        entity.setBankAccountNumber(dto.getBankAccountNumber());
        entity.setBankTranId(dto.getBankTranId());
        entity.setPaymentMode(dto.getPaymentMode());
        entity.setNarration(dto.getNarration());
        entity.setIpAddress(requestHeaderMetaDtoMeta.getIpAddress());

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
            int page,
            int size,
            Long id,
            String requestedBy,
            String mobileNumber,
            Double depositAmount,
            String status,
            LocalDate depositDate,
            LocalDate fromDate,
            LocalDate toDate,
            LocalDate createDateTime,
            String bankHolderName,
            String bankAccountName,
            String bankAccountNumber,
            String bankTranId,
            String paymentMode,
            String narration,
            String ipAddress,
            LocalDate approveOrRejectDate,
            String approveOrRejectTime,
            String remarks,
            String depositType
    ) {

        String loginHierarchyId =
                tokenUtil.extractHierarchyId();

        if (loginHierarchyId == null
                || loginHierarchyId.isBlank()) {

            throw new IllegalArgumentException(
                    "Login hierarchy ID is missing from token"
            );
        }

        loginHierarchyId =
                loginHierarchyId.trim();

        String loginType =
                commonMethods.extractHierarchyType(
                        loginHierarchyId
                );

        if (loginType == null
                || loginType.isBlank()) {

            throw new IllegalArgumentException(
                    "Unable to identify hierarchy type for: "
                            + loginHierarchyId
            );
        }

        loginType = loginType.trim().toUpperCase();

        log.info(
                "Fetching prefund requests | "
                        + "loginHierarchyId={} | loginType={}",
                loginHierarchyId,
                loginType
        );

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(
                        Sort.Direction.DESC,
                        "createDateTime"
                )
        );

        Specification<PrefundRequest> spec =
                PrefundRequestSpecification.getFilter(
                        id,
                        requestedBy,
                        mobileNumber,
                        depositAmount,
                        status,
                        depositDate,
                        fromDate,
                        toDate,
                        createDateTime,
                        bankHolderName,
                        bankAccountName,
                        bankAccountNumber,
                        bankTranId,
                        paymentMode,
                        narration,
                        ipAddress,
                        approveOrRejectDate,
                        approveOrRejectTime,
                        remarks,
                        depositType
                );

        /*
         * Access rules
         *
         * AD:
         * Super admin, can see all records.
         *
         * AU:
         * AD's internal user, works with AD authority.
         * Therefore, AU can also see all records.
         *
         * WLAU:
         * WL's internal user.
         * It gets the authority of its direct parent WL.
         *
         * WL, SD, PT, DT and RT:
         * Use their own hierarchy ID and see only their subtree.
         */
        boolean hasFullSystemAccess =
                "AD".equalsIgnoreCase(loginType)
                        || "AU".equalsIgnoreCase(loginType);

        if (hasFullSystemAccess) {

            log.info(
                    "Full prefund access granted | "
                            + "loginHierarchyId={} | loginType={}",
                    loginHierarchyId,
                    loginType
            );

            /*
             * No hierarchy specification is added.
             *
             * AD and AU receive all records while normal search
             * filters are still applied.
             */

        } else {

            String effectiveHierarchyId =
                    resolveEffectiveHierarchyId(
                            loginHierarchyId,
                            loginType
                    );

            log.info(
                    "Applying prefund hierarchy scope | "
                            + "loginHierarchyId={} | "
                            + "loginType={} | "
                            + "effectiveHierarchyId={}",
                    loginHierarchyId,
                    loginType,
                    effectiveHierarchyId
            );

            spec = spec.and(
                    PrefundRequestSpecification
                            .hierarchyScope(
                                    effectiveHierarchyId
                            )
            );
        }

        Page<PrefundRequest> pageResult =
                repository.findAll(
                        spec,
                        pageable
                );

        log.info(
                "Prefund requests fetched successfully | "
                        + "loginHierarchyId={} | "
                        + "loginType={} | "
                        + "pageNumber={} | "
                        + "pageSize={} | "
                        + "totalElements={} | "
                        + "totalPages={}",
                loginHierarchyId,
                loginType,
                page,
                size,
                pageResult.getTotalElements(),
                pageResult.getTotalPages()
        );

        return pageResult.map(p ->
                PrefundRequestResponseDTO.builder()
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
                        .adminTxnId(p.getAdminTranId())
                        .narration(p.getNarration())
                        .paymentMode(p.getPaymentMode())
                        .ipAddress(p.getIpAddress())
                        .approveOrRejectDate(p.getApproveOrRejectDate())
                        .approveOrRejectTime(p.getApproveOrRejectTime())
                        .remarks(p.getRemarks())
                        .depositType(p.getDepositType())
                        .depositeImage(
                                fileUrlUtil.getFullUrl(
                                        p.getDepositImage()
                                )
                        )
                        .subProductName(
                                p.getSubProducts() != null
                                        ? p.getSubProducts()
                                        .getSubProductTitle()
                                        : null
                        )
                        .productName(
                                p.getSubProducts() != null
                                        && p.getSubProducts()
                                        .getProduct() != null
                                        ? p.getSubProducts()
                                        .getProduct()
                                        .getName()
                                        : null
                        )
                        .schemeName(
                                p.getScheme() != null
                                        ? p.getScheme()
                                        .getSchemeName()
                                        : null
                        )
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

        // ================= UNIFIED RESOLUTION =================

        SubProducts subProduct = fundRequest.getSubProducts();
        Scheme scheme = fundRequest.getScheme();
        Products product = subProduct.getProduct();

        String schemeCode = scheme.getSchemeCode();

        log.info("Resolved from DB | productId={}, subProductId={}, schemeCode={}",
                product.getId(), subProduct.getId(), schemeCode);

        // =========================================================
        // VALIDATE SUBPRODUCT VISIBILITY
        // =========================================================
        if (!subProductsServiceImpl.isSubProductVisible(subProduct.getId()))
        {
            throw new IllegalArgumentException(
                    "Selected sub product is currently not visible."
            );
        }

        log.info("Subproduct is visible and available for wallet transfer. subProductId={}", subProduct.getId());

        // ================= SCHEME RATE =================

        SchemeRate schemeRate = schemeRateRepository
                .findSchemeRateByCodeAndOperatorAndAmount(
                        schemeCode,
                        fundRequest.getBankTransferTo(),
                        fundRequest.getDepositAmount()
                )
                .orElseThrow(() -> new RuntimeException("SchemeRate not found"));

        BigDecimal grossAmount = BigDecimal.valueOf(schemeRate.getGross());

        TransactionDetailsInfoDto transactionDetailsInfo =
                TransactionDetailsInfoDto.builder()
                        .subProductCode(subProduct.getSubProductCode())
                        .schemeName("Prefund")
                        .operatorName(schemeRate.getOperatorName())
                        .vendorName("Internal Product")
                        .build();

        Status newStatus = actionDto.getStatus();
        fundRequest.setStatus(newStatus);
        fundRequest.setRemarks(actionDto.getRemarks());
        fundRequest.setApproveOrRejectDate(date);
        fundRequest.setApproveOrRejectTime(time);
        fundRequest.setAdminTranId("FLD" + baseTransactionId);

        // ================= WALLET FLOW =================
        if (newStatus == Status.APPROVED) {

            fundRequest.setAdminTranId("TXN" + baseTransactionId);

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
                    "Fund Transfer to " + requesterId, subProduct.getSubProductTitle(), scheme.getSchemeName(), fundRequest.getBankTransferTo());

            String requesterType = commonMethods.extractHierarchyType(requesterId);
            String parentHierarchyId = requesterId;

            if ("RU".equalsIgnoreCase(requesterType)) {
                parentHierarchyId = getParentRetailerId(requesterId);
            }

            AbstractWalletAmountTable requesterWallet =
                    commonMethods.getWalletByType(walletType.name(), parentHierarchyId);

            updateWallet(requesterWallet, parentHierarchyId, walletType, amount,
                    "Credit", baseTransactionId, "SUCCESS", "CREDIT",
                    "Fund Transfer from " + approverId, subProduct.getSubProductTitle(), scheme.getSchemeName(), fundRequest.getBankTransferTo());
        }



        repository.save(fundRequest);

        try {

            if (Boolean.TRUE.equals(product.getHasCharges())) {
                walletTransactionService.processHierarchyCharges(
                        fundRequest.getRequestedBy(),
                        product,
                        subProduct,
                        schemeRate,
                        grossAmount,
                        baseTransactionId,
                        transactionDetailsInfo
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
                                walletType,
                                transactionDetailsInfo
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
                                                                    String tranxType, String baseTransactionId, String tranStatus, String service, String remarks,
                                                                    String subProductName, String schemeName, String operatorName) {

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
                wallet.getAvailableBalance(), actionOnBalance, "TXN" + baseTransactionId, service, remarks, subProductName, schemeName, operatorName);

    }

    private void insertTransaction(String walletType, String hierarchyId, String tranStatus, BigDecimal amount, BigDecimal beforeBalance,
                                   BigDecimal afterBalance, String actionOnBalanceAmount, String tranxId, String service, String remarks,
                                   String subProductName, String schemeName, String operatorName) {

        String hierarchyType = commonMethods.extractHierarchyType(hierarchyId);
        String transactionTableName = commonMethods.getTransactionTable(hierarchyType, walletType);
        String columnName = commonMethods.getHierarchyColumnName(hierarchyType);

        log.info("Inserting into {} | Type: {} | Amount: {} | Hierarchy ID: {} | Before Balance: {} | After Balance: {}",
                transactionTableName, service, amount, hierarchyId, beforeBalance, afterBalance);

        String sql = "INSERT INTO " + transactionTableName +
                " (" + columnName + "  ,service, amount, date_of_transaction, time_of_transaction, " +
                "bal_before_tran, bal_after_tran, action_on_balance_amount, final_balance, transaction_id, tran_status, remarks, " +
                "sub_product_name, scheme_name, operator_name) " +
                "VALUES (:hierarchyId, :service, :amount, :dateOfTransaction, :timeOfTransaction, " +
                ":beforeTransactionBalance, :afterTransactionBalance, :actionOnBalanceAmount, :finalBalance, :tranxId, :tranStatus, :remarks," +
                ":subProductName, :schemeName, :operatorName)";

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
                    .setParameter("subProductName", subProductName)
                    .setParameter("schemeName", schemeName)
                    .setParameter("operatorName", operatorName)
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
                    .depositeImage(fileUrlUtil.getFullUrl(entity.getDepositImage()))

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

    private String resolveEffectiveHierarchyId(
            String loginHierarchyId,
            String loginHierarchyType
    ) {

        /*
         * Normal hierarchy accounts use their own ID.
         *
         * Examples:
         * WL -> its own complete subtree
         * SD -> its own complete subtree
         * PT -> its own complete subtree
         * DT -> its own complete subtree
         * RT -> only itself because its depth=0 entry exists
         */
        if (!"WLAU".equalsIgnoreCase(loginHierarchyType)) {
            return loginHierarchyId;
        }

        /*
         * WLAU works on behalf of its direct parent WL.
         *
         * Example:
         * NFT-WLAU-001 -> NFT-WL-001
         */
        AccessHierarchyNode wlauNode =
                accessHierarchyNodeRepository
                        .findById(loginHierarchyId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Hierarchy node not found for WLAU: "
                                                + loginHierarchyId
                                )
                        );

        if (!"WLAU".equalsIgnoreCase(
                wlauNode.getHierarchyType()
        )) {
            throw new IllegalStateException(
                    "Invalid hierarchy node type. "
                            + "Hierarchy ID: "
                            + loginHierarchyId
                            + ", expected type: WLAU"
                            + ", actual type: "
                            + wlauNode.getHierarchyType()
            );
        }

        String parentHierarchyId =
                wlauNode.getParentHierarchyId();

        if (parentHierarchyId == null
                || parentHierarchyId.isBlank()
                || "-".equals(parentHierarchyId.trim())) {

            throw new IllegalStateException(
                    "Parent WL is not configured for WLAU: "
                            + loginHierarchyId
            );
        }

        AccessHierarchyNode parentNode =
                accessHierarchyNodeRepository
                        .findById(parentHierarchyId)
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "Parent hierarchy node not found. "
                                                + "WLAU: "
                                                + loginHierarchyId
                                                + ", parent hierarchy: "
                                                + parentHierarchyId
                                )
                        );

        if (!"WL".equalsIgnoreCase(
                parentNode.getHierarchyType()
        )) {
            throw new IllegalStateException(
                    "WLAU must belong directly to a WL. "
                            + "WLAU: "
                            + loginHierarchyId
                            + ", parent: "
                            + parentHierarchyId
                            + ", parent type: "
                            + parentNode.getHierarchyType()
            );
        }

        log.info(
                "WLAU authority resolved to parent WL | "
                        + "wlauId={} | parentWlId={}",
                loginHierarchyId,
                parentHierarchyId
        );

        return parentHierarchyId;
    }

}
