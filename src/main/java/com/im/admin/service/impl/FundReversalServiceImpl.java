package com.im.admin.service.impl;

import com.im.admin.commons.SMSGatewayAPI;
import com.im.admin.commons.ShivSmsService;
import com.im.admin.dto.*;
import com.im.admin.entities.*;
import com.im.admin.entities.enums.RateType;
import com.im.admin.entities.enums.Status;
import com.im.admin.entities.enums.WalletType;
import com.im.admin.entities.product.Products;
import com.im.admin.repositories.*;
import com.im.admin.response.CommonResponse;
import com.im.admin.response.FundReversalResponseDto;
import com.im.admin.service.FundReversalService;
import com.im.admin.util.CommonMethods;
import com.im.admin.util.OtpUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class FundReversalServiceImpl implements FundReversalService {

    @PersistenceContext
    private EntityManager entityManager;
    @Autowired
    private RetailerRepository retailerRepository;
    @Autowired
    private WhiteLabelUserRepository whiteLabelUserRepository;

    @Autowired
    private DistributorRepository distributorRepository;

    @Autowired
    private PartnerRepository partnerRepository;

    @Autowired
    private SuperDistributorRepository superDistributorRepository;

    @Autowired
    private WhiteLabelRepository whiteLabelsRepository;

    @Autowired
    private EnterprisesRepository enterprisesRepository;

    @Autowired
    private UserDetailsRepository userDetailsRepository;

    @Autowired
    private ShivSmsService shivSmsService;

    @Autowired
    private SmsCredentialsRepository smsCredentialsRepository;


    @Autowired
    private FundReversalRepository fundReversalRepository;

    @Autowired
    private CommonMethods commonMethods;

    @Autowired
    private SMSGatewayAPI smsGatewayAPI;

    @Autowired
    private SubProductsRepository subProductsRepository;

    @Autowired
    private OperatorsRepository operatorsRepository;

    @Autowired
    private TransactionEngineServiceImpl transactionEngineServiceImpl;

    @Autowired
    private WalletToWalletTransferServiceImpl walletToWalletTransferService;

    @Autowired
    private WhiteLabelRepository whiteLabelRepository;

    @Autowired
    private AdminRepository adminRepository;

    @Override
    public CommonResponse<FundReversalResponseDto> resendOtpById(Long id) {
        Optional<FundReversal> optReversal = fundReversalRepository.findById(id);

        if (optReversal.isEmpty()) {
            return new CommonResponse<>(404, "No fund reversal request found with the given ID.", null);
        }

        FundReversal fundReversal = optReversal.get();

        if (Boolean.TRUE.equals(fundReversal.getOtpVerified())) {
            return new CommonResponse<>(400, "OTP already verified. Cannot resend.", null);
        }

        // Allow resend after 30 seconds
        long secondsSinceGenerated = Duration.between(fundReversal.getOtpGeneratedTime(), LocalDateTime.now()).toSeconds();
        if (secondsSinceGenerated < 30) {
            return new CommonResponse<>(400, "Please wait seconds before requesting a new OTP.", null);
        }

        // Generate new OTP
        String newOtp = OtpUtil.generateOtp();
        try {
            FundReversalRequestDto request = new FundReversalRequestDto();
            request.setFundReversalRequestId(fundReversal.getId());
            request.setParentHierarchyId(fundReversal.getParentHierarchyId());
            request.setChildHierarchyId(fundReversal.getChildHierarchyId());
            request.setWalletType(fundReversal.getWalletType());
            request.setReversalAmount(fundReversal.getReversalAmount());

            sendFundReversalOtpSms(request, newOtp);
        } catch (Exception e) {
            log.error(
                    "Failed to send Fund Reversal OTP SMS for ChildHierarchyId: {}.",
                    fundReversal.getChildHierarchyId(),
                    e
            );
        }
        fundReversal.setOtp(newOtp);
        fundReversal.setOtpGeneratedTime(LocalDateTime.now());
        fundReversalRepository.save(fundReversal);

        // TODO: Send new OTP via SMS/Email
        Optional<UserDetails> HierarchyId = userDetailsRepository.findByHierarchyId(fundReversal.getChildHierarchyId());
        UserDetails userDetails = HierarchyId.get();

//        smsGatewayAPI.sendOTPSms(userDetails.getMobileNumber(), newOtp);

        FundReversalResponseDto responseDto = new FundReversalResponseDto(
                fundReversal.getId(),
                newOtp,
                null
        );

        return new CommonResponse<>(200, "New OTP sent successfully. It will expire in 5 minutes.", responseDto);
    }

    @Override
    public String checkBalance(CheckBalanceRequestDto checkBalanceRequestDto) {

        String hierarchyId = checkBalanceRequestDto.getHierarchyId();
        WalletType walletType = checkBalanceRequestDto.getWalletType();
        BigDecimal amount = checkBalanceRequestDto.getAmount();

        AbstractWalletAmountTable wallet = commonMethods.getWalletByType(walletType.name(), hierarchyId);
        BigDecimal balance = wallet.getAvailableBalance();

        if (balance.compareTo(amount) < 0) {
            throw new RuntimeException("Insufficient balance in wallet");
        }

        return "Sufficient balance available";
    }

    @Override
    @Transactional
    public CommonResponse<FundReversalResponseDto> takeActionOnFundReversal(FundReversalRequestDto request, RequestHeaderMetaDto requestHeaderMetaDtoMeta) {

        log.info("===== Starting Fund Reversal Action =====");
        log.info("Request: ParentHierarchyId={}, ChildHierarchyId={}, ReversalAmount={}, WalletType={}",
                request.getParentHierarchyId(),
                request.getChildHierarchyId(),
                request.getReversalAmount(),
                request.getWalletType());
        log.debug("Location - Latitude: {}, Longitude: {}", requestHeaderMetaDtoMeta.getLatitude(), requestHeaderMetaDtoMeta.getLongitude());

        long startTime = System.currentTimeMillis();

        try {
            // Check balance
            log.info("Step 1: Checking balance for ChildHierarchyId: {}, WalletType: {}, Amount: {}",
                    request.getChildHierarchyId(),
                    request.getWalletType(),
                    request.getReversalAmount());

            long balanceCheckStart = System.currentTimeMillis();

            CheckBalanceRequestDto balanceRequest = new CheckBalanceRequestDto();
            balanceRequest.setHierarchyId(request.getChildHierarchyId());
            balanceRequest.setWalletType(request.getWalletType());
            balanceRequest.setAmount(request.getReversalAmount());

            checkBalance(balanceRequest);

            long balanceCheckDuration = System.currentTimeMillis() - balanceCheckStart;
            log.info("Balance check completed successfully in {} ms", balanceCheckDuration);

        } catch (RuntimeException e) {
            log.error("Balance check failed for ChildHierarchyId: {}, Amount: {}",
                    request.getChildHierarchyId(),
                    request.getReversalAmount(),
                    e);
            return new CommonResponse<>(400, e.getMessage(), null);
        }

        // OTP Generation/Verification
        if (request.getOtp() == null || request.getOtp().isEmpty()) {
            log.info("Step 2: No OTP provided. Generating new OTP...");

            String otp = OtpUtil.generateOtp(); // otp
            log.debug("Generated OTP: {} for ChildHierarchyId: {}", otp, request.getChildHierarchyId());
            //opt servcie logic sms
            try {
                sendFundReversalOtpSms(request, otp);
            } catch (Exception e) {
                log.error(
                        "Failed to send Fund Reversal OTP SMS for ChildHierarchyId: {}.",
                        request.getChildHierarchyId(),
                        e
                );
            }
            FundReversal reversal = new FundReversal();
            reversal.setParentHierarchyId(request.getParentHierarchyId());
            reversal.setChildHierarchyId(request.getChildHierarchyId());
            reversal.setReversalAmount(request.getReversalAmount());
            reversal.setWalletType(request.getWalletType());
            reversal.setOtp(otp);
            reversal.setOtpGeneratedTime(LocalDateTime.now());
            reversal.setStatus(Status.PENDING);

            log.debug("Saving FundReversal record with OTP...");
            fundReversalRepository.save(reversal);
            log.info("FundReversal record saved with ID: {}", reversal.getId());

            Optional<UserDetails> userOpt = userDetailsRepository.findByHierarchyId(request.getChildHierarchyId());
            log.debug("User details found: {}", userOpt.isPresent() ? "Yes" : "No");

            FundReversalResponseDto responseDto = new FundReversalResponseDto(reversal.getId(), otp, null);

            log.info("OTP generated successfully for FundReversal ID: {}", reversal.getId());
            log.info("===== Fund Reversal OTP Generation Completed =====");

            return new CommonResponse<>(200,
                    "OTP sent successfully. It will expire in 5 minutes.",
                    responseDto);
        }

        // OTP Verification
        log.info("Step 3: Verifying OTP for ChildHierarchyId: {}", request.getChildHierarchyId());
        log.debug("Provided OTP: {}", request.getOtp());

        Optional<FundReversal> optTxn = fundReversalRepository
                .findTopByParentHierarchyIdAndChildHierarchyIdAndStatusOrderByIdDesc(
                        request.getParentHierarchyId(),
                        request.getChildHierarchyId(),
                        Status.PENDING);

        if (optTxn.isEmpty()) {
            log.warn("No pending transaction found for ParentHierarchyId: {}, ChildHierarchyId: {}",
                    request.getParentHierarchyId(),
                    request.getChildHierarchyId());
            return new CommonResponse<>(404, "No pending transaction found.", null);
        }

        FundReversal fundReversal = optTxn.get();
        log.debug("Found FundReversal record with ID: {}, OTP generated at: {}",
                fundReversal.getId(),
                fundReversal.getOtpGeneratedTime());

        // Validate OTP
        if (!fundReversal.getOtp().equals(request.getOtp())) {
            log.warn("OTP mismatch for FundReversal ID: {}. Provided: {}, Expected: {}",
                    fundReversal.getId(),
                    request.getOtp(),
                    fundReversal.getOtp());
            return new CommonResponse<>(400, "Invalid OTP.", null);
        }
        log.debug("OTP verified successfully");

        // Check OTP expiry
        Duration otpAge = Duration.between(fundReversal.getOtpGeneratedTime(), LocalDateTime.now());
        long minutesElapsed = otpAge.toMinutes();
        log.debug("OTP age: {} minutes", minutesElapsed);

        if (minutesElapsed > 5) {
            log.warn("OTP expired for FundReversal ID: {}. Age: {} minutes",
                    fundReversal.getId(),
                    minutesElapsed);
            return new CommonResponse<>(400, "OTP expired.", null);
        }

        // Process wallet transfer
        BigDecimal reverseAmount = fundReversal.getReversalAmount();
        WalletType walletType = fundReversal.getWalletType();
        String baseTransactionId = commonMethods.generateBaseTransactionId();

        log.info("Step 4: Processing wallet transfer");
        log.info("Transaction Details - Amount: {}, WalletType: {}, BaseTransactionId: {}",
                reverseAmount,
                walletType,
                baseTransactionId);

        long walletTransferStart = System.currentTimeMillis();

        processWalletTransfer(request, reverseAmount, walletType, baseTransactionId, requestHeaderMetaDtoMeta);

        long walletTransferDuration = System.currentTimeMillis() - walletTransferStart;
        log.info("Wallet transfer completed successfully in {} ms", walletTransferDuration);

        // Update FundReversal status
        log.info("Step 5: Updating FundReversal status to APPROVED");
        fundReversal.setStatus(Status.APPROVED);
        fundReversal.setOtpVerified(true);
        fundReversal.setActionDate(LocalDate.now());
        fundReversal.setActionTime(LocalTime.now());

        fundReversalRepository.save(fundReversal);
        log.debug("FundReversal record updated with ID: {}", fundReversal.getId());

        // Process charges
        log.info("Step 6: Processing charges");
        long chargesStart = System.currentTimeMillis();

        try {
            SubProducts subProduct = getSubProduct(request.getSubProductId());
            Products product = subProduct.getProduct();
            log.debug("Product: {}, SubProduct: {}", product.getName(), subProduct.getSubProductTitle());

            SchemeRate schemeRate = walletToWalletTransferService
                    .getExactSchemeRate(request.getSchemeCode(), reverseAmount.doubleValue());
            log.debug("Scheme Rate obtained: {}", schemeRate);

            BigDecimal grossCharge = calculateGrossCharge(reverseAmount, schemeRate);
            log.info("Gross Charge calculated: {} for Amount: {}", grossCharge, reverseAmount);

            processHierarchyCharges(
                    request,
                    fundReversal,
                    walletType,
                    product,
                    subProduct,
                    schemeRate,
                    grossCharge,
                    baseTransactionId
            );

            long chargesDuration = System.currentTimeMillis() - chargesStart;
            log.info("Charges processed successfully in {} ms", chargesDuration);

        } catch (Exception e) {
            log.error("Failed to process charges for BaseTransactionId: {}", baseTransactionId, e);
        }

        FundReversalResponseDto responseDto = new FundReversalResponseDto(
                fundReversal.getId(),
                null,
                "TXN" + baseTransactionId
        );

        long totalDuration = System.currentTimeMillis() - startTime;
        log.info("===== Fund Reversal Action Completed Successfully =====");
        log.info("Summary:");
        log.info("  - FundReversal ID: {}", fundReversal.getId());
        log.info("  - Base Transaction ID: {}", baseTransactionId);
        log.info("  - Amount: {}", reverseAmount);
        log.info("  - Wallet Type: {}", walletType);
        log.info("  - Status: APPROVED");
        log.info("  - Total Duration: {} ms", totalDuration);

        return new CommonResponse<>(200,
                "Transaction successful. Amount debited.",
                responseDto);
    }

    private void processWalletTransfer(
            FundReversalRequestDto request,
            BigDecimal amount,
            WalletType walletType,
            String baseTransactionId,
            RequestHeaderMetaDto requestHeaderMetaDto) {

        AbstractWalletAmountTable fromWallet =
                commonMethods.getWalletByType(
                        walletType.name(),
                        request.getChildHierarchyId());

        RetailersExtraColumnsDto retailerExtraColumns =
                RetailersExtraColumnsDto.builder()
                        .medium(request.getMedium())
                        .clientIp(requestHeaderMetaDto.getIpAddress())
                        .requestDomainName(request.getRequestDomainName())
                        .latitude(requestHeaderMetaDto.getLatitude())
                        .longitude(requestHeaderMetaDto.getLongitude())
                        .build();

        commonMethods.updateWallet(
                fromWallet,
                request.getChildHierarchyId(),
                walletType,
                amount,
                "Debit",
                baseTransactionId,
                "SUCCESS",
                "DEBIT",
                "Fund Reversal",
                "Payment",
                "Fund Reverse to " + request.getParentHierarchyId(),
                "",
                retailerExtraColumns,
                "Internal Product",
                null
        );

        AbstractWalletAmountTable toWallet =
                commonMethods.getWalletByType(
                        walletType.name(),
                        request.getParentHierarchyId());

        commonMethods.updateWallet(
                toWallet,
                request.getParentHierarchyId(),
                walletType,
                amount,
                "Credit",
                baseTransactionId,
                "SUCCESS",
                "CREDIT",
                "Fund Reversal",
                "Payment",
                "Fund Reversed By " + request.getChildHierarchyId(),
                "",
                retailerExtraColumns,
                null,
                null
        );
    }

    private BigDecimal calculateGrossCharge(
            BigDecimal reverseAmount,
            SchemeRate schemeRate) {

        if (schemeRate.getRateType() == RateType.PERCENTAGE) {

            return reverseAmount
                    .multiply(BigDecimal.valueOf(schemeRate.getGross()))
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        }

        return BigDecimal.valueOf(schemeRate.getGross());
    }

    private SubProducts getSubProduct(String id) {

        return subProductsRepository
                .findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "SubProduct not found for subProductName: "
                                        + "Fund Reversal"));
    }

    private TransactionSchemeDetailsRequestDto buildTransactionDto(
            String baseTransactionId,
            FundReversal fundReversal,
            SchemeRate schemeRate,
            Products product,
            SubProducts subProduct,
            String hierarchyId,
            WalletType walletType) {

        log.debug("Building transaction DTO for baseTransactionId: {}, hierarchyId: {}, walletType: {}",
                baseTransactionId, hierarchyId, walletType);

        TransactionSchemeDetailsRequestDto dto =
                new TransactionSchemeDetailsRequestDto();

        dto.setBaseTranxId(baseTransactionId);
        dto.setAmount(fundReversal.getReversalAmount().doubleValue());
        dto.setConfirmAmount(fundReversal.getReversalAmount().doubleValue());
        dto.setOperatorName(schemeRate.getOperatorName());
        dto.setOperatorCode(schemeRate.getSchemeCode());
        dto.setSchemeRate(schemeRate);
        dto.setTransactionType(product.getTransactionType());
        dto.setWalletType(walletType.name());
        dto.setGstValue(String.valueOf(product.getSgst()));
        dto.setTdsRate(String.valueOf(product.getTds()));
        dto.setHierarchyId(hierarchyId);
        dto.setProductCode(product.getProductCode());
        dto.setSubProductCode(subProduct.getSubProductCode());
        dto.setIsCommission(product.getIsCommissions());
        dto.setHasCharge(product.getHasCharges());

        log.debug("Transaction DTO built successfully for hierarchy: {}", hierarchyId);
        return dto;
    }

    private void processHierarchyCharges(
            FundReversalRequestDto request,
            FundReversal fundReversal,
            WalletType walletType,
            Products product,
            SubProducts subProduct,
            SchemeRate schemeRate,
            BigDecimal grossCharge,
            String baseTransactionId) {

        log.info("Processing hierarchy charges - baseTransactionId: {}, walletType: {}, grossCharge: {}",
                baseTransactionId, walletType, grossCharge);

        String currentId = request.getParentHierarchyId();
        log.debug("Starting hierarchy traversal from currentId: {}", currentId);

        while (true) {

            Optional<String> parentOpt =
                    commonMethods.findParentHierarchy(currentId);

            if (parentOpt.isEmpty()) {
                log.error("No approver found for hierarchy: {}", currentId);
                throw new RuntimeException(
                        "No approver found for hierarchy: " + currentId);
            }

            String parentId = parentOpt.get();
            String parentType =
                    commonMethods.extractHierarchyType(parentId);

            log.debug("Found parent hierarchy - id: {}, type: {}", parentId, parentType);

            if ("WL".equals(parentType) ||
                    "IM".equals(parentType) ||
                    "AD".equals(parentType)) {

                log.info("Processing charges for hierarchy type: {}, id: {}", parentType, parentId);
                processChargesForHierarchy(
                        request,
                        fundReversal,
                        walletType,
                        product,
                        subProduct,
                        schemeRate,
                        grossCharge,
                        baseTransactionId,
                        parentId
                );
                log.info("Charges processed successfully for hierarchy: {}", parentId);
                break;
            }

            currentId = parentId;
            log.debug("Continuing hierarchy traversal to parent: {}", currentId);
        }
    }

    private void processChargesForHierarchy(
            FundReversalRequestDto request,
            FundReversal fundReversal,
            WalletType walletType,
            Products product,
            SubProducts subProduct,
            SchemeRate schemeRate,
            BigDecimal grossCharge,
            String baseTransactionId,
            String hierarchyId) {

        log.info("Processing charges for hierarchy - hierarchyId: {}, walletType: {}, baseTransactionId: {}, grossCharge: {}",
                hierarchyId, walletType, baseTransactionId, grossCharge);

        /* ================= PARENT HIERARCHY (DEBIT) ================= */

        TransactionSchemeDetailsRequestDto dto =
                buildTransactionDto(
                        baseTransactionId,
                        fundReversal,
                        schemeRate,
                        product,
                        subProduct,
                        request.getParentHierarchyId(),
                        walletType
                );

        BigDecimal grossAmount =
                transactionEngineServiceImpl.calculateGross(dto, grossCharge);
        log.debug("Calculated gross amount: {} for hierarchy: {}", grossAmount, request.getParentHierarchyId());

        BigDecimal gstRate =
                BigDecimal.valueOf(product.getSgst())
                        .divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP);
        log.debug("GST rate: {} for product: {}", gstRate, product.getProductCode());

        BigDecimal baseAmount =
                grossAmount.divide(BigDecimal.ONE.add(gstRate), 2, RoundingMode.HALF_UP);

        BigDecimal gstAmount =
                grossAmount.subtract(baseAmount).setScale(2, RoundingMode.HALF_UP);
        log.debug("Calculated amounts - baseAmount: {}, gstAmount: {}, grossAmount: {}",
                baseAmount, gstAmount, grossAmount);

        String parentHierarchyType =
                commonMethods.extractHierarchyType(request.getParentHierarchyId());

        String parentTable =
                transactionEngineServiceImpl.getTransactionTable(parentHierarchyType, walletType.name());
        log.debug("Parent hierarchy - type: {}, table: {}", parentHierarchyType, parentTable);

        AbstractWalletAmountTable parentWallet =
                commonMethods.getWalletByType(walletType.name(), request.getParentHierarchyId());

        BigDecimal parentBalance =
                parentWallet.getAvailableBalance().setScale(2, RoundingMode.HALF_UP);
        log.debug("Parent wallet balance for {}: {}", request.getParentHierarchyId(), parentBalance);

        /* CHARGE DEBIT */

        BigDecimal afterChargeDebit =
                parentBalance.subtract(baseAmount).setScale(2, RoundingMode.HALF_UP);
        log.debug("After charge debit balance: {} for parent: {}", afterChargeDebit, request.getParentHierarchyId());

        transactionEngineServiceImpl.insertTransactionForHierarchy(
                parentTable,
                "DEBIT",
                baseAmount,
                "Charges Debited",
                request.getParentHierarchyId(),
                walletType.name(),
                parentBalance,
                afterChargeDebit,
                "Deduct Funds (Charge)",
                "CHG" + baseTransactionId,
                "SUCCESS",
                dto
        );
        log.info("Charge debit transaction inserted successfully for parent: {}", request.getParentHierarchyId());

        /* GST DEBIT */

        BigDecimal afterGstDebit =
                afterChargeDebit.subtract(gstAmount).setScale(2, RoundingMode.HALF_UP);
        log.debug("After GST debit balance: {} for parent: {}", afterGstDebit, request.getParentHierarchyId());

        transactionEngineServiceImpl.insertTransactionForHierarchy(
                parentTable,
                "DEBIT",
                gstAmount,
                "GST Debited",
                request.getParentHierarchyId(),
                walletType.name(),
                afterChargeDebit,
                afterGstDebit,
                "Deduct Funds (GST)",
                "GST" + baseTransactionId,
                "SUCCESS",
                dto
        );
        log.info("GST debit transaction inserted successfully for parent: {}", request.getParentHierarchyId());


        /* ================= WL / ADMIN HIERARCHY (CREDIT) ================= */

        log.debug("Processing credit transactions for hierarchy: {}", hierarchyId);

        TransactionSchemeDetailsRequestDto dto2 =
                buildTransactionDto(
                        baseTransactionId,
                        fundReversal,
                        schemeRate,
                        product,
                        subProduct,
                        hierarchyId,
                        walletType
                );

        String hierarchyType2 =
                commonMethods.extractHierarchyType(hierarchyId);

        String tableName2 =
                transactionEngineServiceImpl.getTransactionTable(hierarchyType2, walletType.name());
        log.debug("Credit hierarchy - type: {}, table: {}", hierarchyType2, tableName2);

        AbstractWalletAmountTable wallet =
                commonMethods.getWalletByType(walletType.name(), hierarchyId);

        BigDecimal walletBalance =
                wallet.getAvailableBalance().setScale(2, RoundingMode.HALF_UP);
        log.debug("Wallet balance for {}: {}", hierarchyId, walletBalance);

        /* CHARGE CREDIT */

        BigDecimal afterChargeCredit =
                walletBalance.add(baseAmount).setScale(2, RoundingMode.HALF_UP);
        log.debug("After charge credit balance: {} for hierarchy: {}", afterChargeCredit, hierarchyId);

        transactionEngineServiceImpl.insertTransactionForHierarchy(
                tableName2,
                "CREDIT",
                baseAmount,
                "Charges Credited",
                hierarchyId,
                walletType.name(),
                walletBalance,
                afterChargeCredit,
                "Add Funds (Charge)",
                "CHG" + baseTransactionId,
                "SUCCESS",
                dto2
        );
        log.info("Charge credit transaction inserted successfully for hierarchy: {}", hierarchyId);

        /* GST CREDIT */

        BigDecimal afterGstCredit =
                afterChargeCredit.add(gstAmount).setScale(2, RoundingMode.HALF_UP);
        log.debug("After GST credit balance: {} for hierarchy: {}", afterGstCredit, hierarchyId);

        transactionEngineServiceImpl.insertTransactionForHierarchy(
                tableName2,
                "CREDIT",
                gstAmount,
                "GST Credited",
                hierarchyId,
                walletType.name(),
                afterChargeCredit,
                afterGstCredit,
                "Add Funds (GST)",
                "GST" + baseTransactionId,
                "SUCCESS",
                dto2
        );
        log.info("GST credit transaction inserted successfully for hierarchy: {}", hierarchyId);
        log.info("Completed processing all charges for hierarchy: {}", hierarchyId);
    }


    private SmsCredentialsMaster getSmsCredentials(String hierarchyType, String hierarchyId, String smsType) {
        String fromId = resolveFromEmail(hierarchyType, hierarchyId);
        return smsCredentialsRepository.findByWhitelabelIdAndType(fromId, smsType)
                .orElseGet(() -> smsCredentialsRepository.findByWhitelabelIdAndType("NFT-AD-001", smsType)
                        .orElseThrow(() -> new RuntimeException("SMS credentials not found for type: " + smsType)));
    }

    private String resolveFromEmail(String hierarchyType, String hierarchyId) {

        switch (hierarchyType) {

            case "RT" -> {
                Retailer rt = retailerRepository.findById(hierarchyId)
                        .orElseThrow(() -> new RuntimeException("Retailer not found"));

                String parentId = rt.getParentAccount();
                String parentType = commonMethods.extractHierarchyType(parentId);

                return resolveFromEmail(parentType, parentId);
            }

            case "DT" -> {
                Distributor dt = distributorRepository.findById(hierarchyId)
                        .orElseThrow(() -> new RuntimeException("Distributor not found"));

                String parentId = dt.getParentAccount();
                String parentType = commonMethods.extractHierarchyType(parentId);

                return resolveFromEmail(parentType, parentId);
            }

            case "PT" -> {
                Partner pt = partnerRepository.findById(hierarchyId)
                        .orElseThrow(() -> new RuntimeException("Partner not found"));

                String parentId = pt.getParentAccount();
                String parentType = commonMethods.extractHierarchyType(parentId);

                return resolveFromEmail(parentType, parentId);
            }

            case "SD" -> {
                SuperDistributor sd = superDistributorRepository.findById(hierarchyId)
                        .orElseThrow(() -> new RuntimeException("SuperDistributor not found"));

                String parentId = sd.getParentAccount();
                String parentType = commonMethods.extractHierarchyType(parentId);

                return resolveFromEmail(parentType, parentId);
            }

            case "WL" -> {
                WhiteLabels wl = whiteLabelsRepository.findById(hierarchyId)
                        .orElseThrow(() -> new RuntimeException("WhiteLabel not found"));

                if (Boolean.TRUE.equals(wl.getIsAdmin())) {
                    return wl.getWhiteLabelId();
                }

                String parentId = wl.getParentAccount();
                String parentType = commonMethods.extractHierarchyType(parentId);

                return resolveFromEmail(parentType, parentId);
            }

            case "WLAU" -> {
                Optional<WhiteLabelUser> wlauOpt =
                        whiteLabelUserRepository.findById(hierarchyId);

                if (wlauOpt.isEmpty()) {
                    return wlauOpt.get().getId();
                }

                WhiteLabelUser wlau = wlauOpt.get();
                String parentId = wlau.getParentAccount();
                String parentType = commonMethods.extractHierarchyType(parentId);

                return resolveFromEmail(parentType, parentId);

            }

            case "ET" -> {
                Enterprises et = enterprisesRepository.findById(hierarchyId)
                        .orElseThrow(() -> new RuntimeException("Enterprise not found"));

                String parentId = et.getParentAccount();
                String parentType = commonMethods.extractHierarchyType(parentId);

                return resolveFromEmail(parentType, parentId);
            }

            case "AD", "IM" -> {
                Admin ad = adminRepository.findById(hierarchyId)
                        .orElseThrow(() -> new RuntimeException("Admin not found"));

                return ad.getId();
            }

            default -> throw new RuntimeException("Invalid hierarchy type: " + hierarchyType);
        }
    }

    private void sendFundReversalOtpSms(
            FundReversalRequestDto request,
            String otp
    ) {

        // Find child user
        UserDetails childUser =
                userDetailsRepository
                        .findByHierarchyId(
                                request.getChildHierarchyId()
                        )
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Child user not found"
                                )
                        );

        String mobileNumber =
                childUser.getMobileNumber();

        if (mobileNumber == null
                || mobileNumber.isBlank()) {

            throw new RuntimeException(
                    "Mobile number not found for child user"
            );
        }

        // Get parent hierarchy type
        String hierarchyType =
                commonMethods.extractHierarchyType(
                        request.getParentHierarchyId()
                );

        String entityName =
                commonMethods.getEntityName(
                        hierarchyType
                );

        // Get primary key column dynamically
        String idColumnName =
                switch (hierarchyType) {
                    case "RT" -> "retailer_id";
                    case "DT" -> "distributor_id";
                    case "PT" -> "partner_id";
                    case "SD" -> "super_distributor_id";
                    case "WL" -> "white_label_id";
                    case "ET" -> "enterprise_id";
                    case "AD" -> "id";
                    default -> "id";
                };

        String sql =
                "SELECT outlet_name FROM "
                        + entityName
                        + " WHERE "
                        + idColumnName
                        + " = :hierarchyId";

        Query query =
                entityManager.createNativeQuery(
                        sql
                );

        query.setParameter(
                "hierarchyId",
                request.getParentHierarchyId()
        );

        Object result =
                query.getResultStream()
                        .findFirst()
                        .orElse(null);

        String parentOutletName =
                result != null
                        ? result.toString()
                        : "User";

        // Get SMS configuration
        SmsCredentialsMaster smsConfig =
                getSmsCredentials(
                        childUser.getHierarchyCode(),
                        childUser.getHierarchyId(),
                        "FUND_REVERSAL"
                );

        // Send SMS
        String smsResponse =
                shivSmsService.sendFundReversalOtpSms(
                        smsConfig,
                        mobileNumber,
                        otp,
                        request.getWalletType().name(),
                        "Reverse",
                        parentOutletName,
                        request.getReversalAmount()
                );

        log.info(
                "Fund Reversal OTP SMS sent to mobile: {}. Response: {}",
                mobileNumber,
                smsResponse
        );
    }

}
