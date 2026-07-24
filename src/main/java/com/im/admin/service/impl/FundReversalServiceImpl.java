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

    @Autowired
    private SchemeRepository schemeRepository;

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
    public CommonResponse<FundReversalResponseDto> takeActionOnFundReversal(
            FundReversalRequestDto request,
            RequestHeaderMetaDto requestHeaderMetaDtoMeta
    ) {

        long startTime = System.currentTimeMillis();

        log.info("===== Starting Fund Reversal Action =====");

        try {

            // ============================================================
            // STEP 0: BASIC REQUEST VALIDATION
            // ============================================================

            if (request == null) {
                log.warn("Fund reversal request is null");
                return new CommonResponse<>(
                        400,
                        "Request body is required.",
                        null
                );
            }

            if (request.getParentHierarchyId() == null ||
                    request.getParentHierarchyId().trim().isEmpty()) {

                log.warn("Parent hierarchy ID is missing");

                return new CommonResponse<>(
                        400,
                        "Parent hierarchy ID is required.",
                        null
                );
            }

            if (request.getChildHierarchyId() == null || request.getChildHierarchyId().trim().isEmpty()) {

                log.warn("Child hierarchy ID is missing");

                return new CommonResponse<>(
                        400,
                        "Child hierarchy ID is required.",
                        null
                );
            }

            if (request.getReversalAmount() == null || request.getReversalAmount().compareTo(BigDecimal.ZERO) <= 0) {

                log.warn(
                        "Invalid reversal amount received. ChildHierarchyId: {}, Amount: {}",
                        request.getChildHierarchyId(),
                        request.getReversalAmount()
                );

                return new CommonResponse<>(
                        400,
                        "Reversal amount must be greater than zero.",
                        null
                );
            }

            if (request.getWalletType() == null) {

                log.warn( "Wallet type is missing. ChildHierarchyId: {}", request.getChildHierarchyId());

                return new CommonResponse<>(
                        400,
                        "Wallet type is required.",
                        null
                );
            }

            if (request.getSubProductId() == null) {

                log.warn("Subproduct ID is missing. ChildHierarchyId: {}",request.getChildHierarchyId());

                return new CommonResponse<>(
                        400,
                        "Subproduct ID is required.",
                        null
                );
            }

            if (request.getSchemeCode() == null || request.getSchemeCode().trim().isEmpty()) {

                log.warn("Scheme code is missing. ChildHierarchyId: {}",request.getChildHierarchyId());

                return new CommonResponse<>(
                        400,
                        "Scheme code is required.",
                        null
                );
            }

            String parentHierarchyId = request.getParentHierarchyId().trim();

            String childHierarchyId = request.getChildHierarchyId().trim();

            String schemeCode = request.getSchemeCode().trim();

            Scheme scheme = schemeRepository.findBySchemeCode(schemeCode)
                    .orElseThrow(() -> new RuntimeException("Invalid Scheme Code: " + schemeCode));

            String schemeName = scheme.getSchemeName();

            BigDecimal requestedAmount = request.getReversalAmount();

            WalletType requestedWalletType = request.getWalletType();

            log.info(
                    "Fund reversal request received. " +
                            "ParentHierarchyId: {}, ChildHierarchyId: {}, " +
                            "ReversalAmount: {}, WalletType: {}, " +
                            "SubProductId: {}, SchemeName: {}",
                    parentHierarchyId,
                    childHierarchyId,
                    requestedAmount,
                    requestedWalletType,
                    request.getSubProductId(),
                    schemeName
            );

            if (requestHeaderMetaDtoMeta != null) {
                log.debug(
                        "Fund reversal request metadata. Latitude: {}, Longitude: {}",
                        requestHeaderMetaDtoMeta.getLatitude(),
                        requestHeaderMetaDtoMeta.getLongitude()
                );
            }

            // ============================================================
            // STEP 1: CHECK CHILD WALLET BALANCE
            // ============================================================

            log.info(
                    "Step 1: Checking wallet balance. " +
                            "ChildHierarchyId: {}, WalletType: {}, Amount: {}",
                    childHierarchyId,
                    requestedWalletType,
                    requestedAmount
            );

            CheckBalanceRequestDto balanceRequest = new CheckBalanceRequestDto();

            balanceRequest.setHierarchyId(childHierarchyId);
            balanceRequest.setWalletType(requestedWalletType);
            balanceRequest.setAmount(requestedAmount);

            try {

                checkBalance(balanceRequest);

                log.info(
                        "Wallet balance check completed successfully. " +
                                "ChildHierarchyId: {}, WalletType: {}, Amount: {}",
                        childHierarchyId,
                        requestedWalletType,
                        requestedAmount
                );

            } catch (RuntimeException e) {

                log.warn(
                        "Wallet balance check failed. " +
                                "ChildHierarchyId: {}, WalletType: {}, Amount: {}, Reason: {}",
                        childHierarchyId,
                        requestedWalletType,
                        requestedAmount,
                        e.getMessage()
                );

                return new CommonResponse<>(
                        400,
                        e.getMessage(),
                        null
                );
            }

            // ============================================================
            // STEP 2: GENERATE OTP
            // ============================================================

            if (request.getOtp() == null || request.getOtp().trim().isEmpty()) {

                log.info(
                        "Step 2: Generating fund reversal OTP. " +
                                "ParentHierarchyId: {}, ChildHierarchyId: {}",
                        parentHierarchyId,
                        childHierarchyId
                );

                String otp = OtpUtil.generateOtp();

                try {

                    sendFundReversalOtpSms(request, otp);

                    log.info(
                            "Fund reversal OTP SMS sent successfully. " +
                                    "ChildHierarchyId: {}",
                            childHierarchyId
                    );

                } catch (Exception e) {

                    log.error(
                            "Failed to send fund reversal OTP SMS. " +
                                    "ChildHierarchyId: {}",
                            childHierarchyId,
                            e
                    );

                    /*
                     * Do not save a usable OTP if SMS delivery failed.
                     * This avoids creating pending transactions for OTPs
                     * that the user never received.
                     */
                    return new CommonResponse<>(
                            500,
                            "Unable to send OTP. Please try again.",
                            null
                    );
                }

                FundReversal reversal = new FundReversal();

                reversal.setParentHierarchyId(parentHierarchyId);
                reversal.setChildHierarchyId(childHierarchyId);
                reversal.setReversalAmount(requestedAmount);
                reversal.setWalletType(requestedWalletType);
                reversal.setOtp(otp);
                reversal.setOtpGeneratedTime(LocalDateTime.now());
                reversal.setStatus(Status.PENDING);
                reversal.setOtpVerified(false);

                FundReversal savedReversal =
                        fundReversalRepository.save(reversal);

                log.info(
                        "Pending fund reversal created successfully. " +
                                "FundReversalId: {}, ParentHierarchyId: {}, ChildHierarchyId: {}",
                        savedReversal.getId(),
                        parentHierarchyId,
                        childHierarchyId
                );

                /*
                 * For production security, OTP should normally not be returned
                 * in the API response. It should only be delivered through SMS.
                 *
                 * Therefore, null is passed instead of otp.
                 */
                FundReversalResponseDto responseDto =
                        new FundReversalResponseDto(
                                savedReversal.getId(),
                                null,
                                null
                        );

                return new CommonResponse<>(
                        200,
                        "OTP sent successfully. It will expire in 5 minutes.",
                        responseDto
                );
            }

            // ============================================================
            // STEP 3: FIND PENDING FUND REVERSAL
            // ============================================================

            log.info(
                    "Step 3: Verifying fund reversal OTP. " +
                            "ParentHierarchyId: {}, ChildHierarchyId: {}",
                    parentHierarchyId,
                    childHierarchyId
            );

            Optional<FundReversal> optionalFundReversal =
                    fundReversalRepository
                            .findTopByParentHierarchyIdAndChildHierarchyIdAndStatusOrderByIdDesc(
                                    parentHierarchyId,
                                    childHierarchyId,
                                    Status.PENDING
                            );

            if (optionalFundReversal.isEmpty()) {

                log.warn(
                        "No pending fund reversal found. " +
                                "ParentHierarchyId: {}, ChildHierarchyId: {}",
                        parentHierarchyId,
                        childHierarchyId
                );

                return new CommonResponse<>(
                        404,
                        "No pending fund reversal transaction found.",
                        null
                );
            }

            FundReversal fundReversal =
                    optionalFundReversal.get();

            if (Boolean.TRUE.equals(fundReversal.getOtpVerified())) {

                log.warn(
                        "Fund reversal OTP has already been verified. FundReversalId: {}",
                        fundReversal.getId()
                );

                return new CommonResponse<>(
                        409,
                        "This fund reversal has already been processed.",
                        null
                );
            }

            if (fundReversal.getStatus() != Status.PENDING) {

                log.warn(
                        "Fund reversal is not in pending status. " +
                                "FundReversalId: {}, Status: {}",
                        fundReversal.getId(),
                        fundReversal.getStatus()
                );

                return new CommonResponse<>(
                        409,
                        "This fund reversal is no longer pending.",
                        null
                );
            }

            // ============================================================
            // STEP 4: VALIDATE REQUEST AGAINST SAVED REVERSAL
            // ============================================================

            /*
             * The amount and wallet type must come from the saved pending
             * transaction. The frontend must not be allowed to alter them
             * while submitting the OTP.
             */

            if (fundReversal.getReversalAmount() == null ||
                    fundReversal.getReversalAmount()
                            .compareTo(requestedAmount) != 0) {

                log.warn(
                        "Fund reversal amount mismatch. " +
                                "FundReversalId: {}, SavedAmount: {}, RequestedAmount: {}",
                        fundReversal.getId(),
                        fundReversal.getReversalAmount(),
                        requestedAmount
                );

                return new CommonResponse<>(
                        400,
                        "Reversal amount does not match the pending transaction.",
                        null
                );
            }

            if (fundReversal.getWalletType() != requestedWalletType) {

                log.warn(
                        "Fund reversal wallet type mismatch. " +
                                "FundReversalId: {}, SavedWalletType: {}, RequestedWalletType: {}",
                        fundReversal.getId(),
                        fundReversal.getWalletType(),
                        requestedWalletType
                );

                return new CommonResponse<>(
                        400,
                        "Wallet type does not match the pending transaction.",
                        null
                );
            }

            // ============================================================
            // STEP 5: VALIDATE OTP
            // ============================================================

            String providedOtp =
                    request.getOtp().trim();

            if (fundReversal.getOtp() == null ||
                    !fundReversal.getOtp().equals(providedOtp)) {

                log.warn(
                        "Invalid OTP received for fund reversal. FundReversalId: {}",
                        fundReversal.getId()
                );

                return new CommonResponse<>(
                        400,
                        "Invalid OTP.",
                        null
                );
            }

            if (fundReversal.getOtpGeneratedTime() == null) {

                log.error(
                        "OTP generated time is missing. FundReversalId: {}",
                        fundReversal.getId()
                );

                return new CommonResponse<>(
                        500,
                        "OTP information is invalid. Please generate a new OTP.",
                        null
                );
            }

            LocalDateTime otpExpiryTime =
                    fundReversal.getOtpGeneratedTime().plusMinutes(5);

            if (LocalDateTime.now().isAfter(otpExpiryTime)) {

                log.warn(
                        "Fund reversal OTP expired. " +
                                "FundReversalId: {}, GeneratedAt: {}, ExpiredAt: {}",
                        fundReversal.getId(),
                        fundReversal.getOtpGeneratedTime(),
                        otpExpiryTime
                );

                return new CommonResponse<>(
                        400,
                        "OTP expired. Please generate a new OTP.",
                        null
                );
            }

            log.info(
                    "Fund reversal OTP verified successfully. FundReversalId: {}",
                    fundReversal.getId()
            );

            // ============================================================
            // STEP 6: FETCH PRODUCT AND SUBPRODUCT
            // ============================================================

            log.info(
                    "Step 6: Fetching transaction product details. " +
                            "FundReversalId: {}, SubProductId: {}",
                    fundReversal.getId(),
                    request.getSubProductId()
            );

            SubProducts subProduct;

            try {

                subProduct =
                        getSubProduct(request.getSubProductId());

            } catch (Exception e) {

                log.warn(
                        "Unable to fetch subproduct. " +
                                "FundReversalId: {}, SubProductId: {}, Reason: {}",
                        fundReversal.getId(),
                        request.getSubProductId(),
                        e.getMessage()
                );

                return new CommonResponse<>(
                        400,
                        "Invalid subproduct: " + e.getMessage(),
                        null
                );
            }

            if (subProduct == null) {

                log.warn(
                        "Subproduct not found. " +
                                "FundReversalId: {}, SubProductId: {}",
                        fundReversal.getId(),
                        request.getSubProductId()
                );

                return new CommonResponse<>(
                        404,
                        "Subproduct not found.",
                        null
                );
            }

            Products product =
                    subProduct.getProduct();

            if (product == null) {

                log.error(
                        "Product is not mapped with subproduct. " +
                                "SubProductId: {}, SubProductCode: {}",
                        request.getSubProductId(),
                        subProduct.getSubProductCode()
                );

                return new CommonResponse<>(
                        400,
                        "Product is not configured for the selected subproduct.",
                        null
                );
            }

            // ============================================================
            // STEP 7: FETCH SCHEME RATE
            // ============================================================

            BigDecimal reverseAmount = fundReversal.getReversalAmount();

            WalletType walletType = fundReversal.getWalletType();

            SchemeRate schemeRate;

            try {

                schemeRate =
                        walletToWalletTransferService.getExactSchemeRate(
                                schemeCode,
                                reverseAmount.doubleValue()
                        );

            } catch (Exception e) {

                log.warn(
                        "Unable to fetch scheme rate. " +
                                "FundReversalId: {}, SchemeCode: {}, Amount: {}, Reason: {}",
                        fundReversal.getId(),
                        schemeCode,
                        reverseAmount,
                        e.getMessage()
                );

                return new CommonResponse<>(
                        400,
                        "Unable to find a valid scheme rate: " + e.getMessage(),
                        null
                );
            }

            if (schemeRate == null) {

                log.warn(
                        "Scheme rate not found. " +
                                "FundReversalId: {}, SchemeCode: {}, Amount: {}",
                        fundReversal.getId(),
                        schemeCode,
                        reverseAmount
                );

                return new CommonResponse<>(
                        404,
                        "Scheme rate not found for the entered amount.",
                        null
                );
            }

            // ============================================================
            // STEP 8: PREPARE TRANSACTION METADATA
            // ============================================================

            String productName = product.getName();

            String subProductCode = subProduct.getSubProductCode();

            /*
             * The uploaded code does not show an Operator object or operator ID
             * in FundReversalRequestDto. Therefore, an internal operator value
             * is used instead of referencing a nonexistent operator variable.
             *
             * Scheme code is available directly from the request. Since the
             * exact SchemeRate-to-Scheme entity relation was not provided,
             * schemeCode is also used as the safe schemeName value.
             */
            TransactionDetailsInfoDto transactionDetailsInfo =
                    TransactionDetailsInfoDto.builder()
                            .subProductCode(subProductCode)
                            .schemeName(schemeName)
                            .operatorName(schemeRate.getOperatorName())
                            .vendorName("Internal Product")
                            .build();

            log.info(
                    "Transaction metadata prepared. " +
                            "FundReversalId: {}, ProductName: {}, " +
                            "SubProductCode: {}, SchemeCode: {}",
                    fundReversal.getId(),
                    productName,
                    subProductCode,
                    schemeCode
            );

            // ============================================================
            // STEP 9: RECHECK BALANCE BEFORE FINAL TRANSFER
            // ============================================================

            /*
             * The first balance check happened before OTP generation.
             * The balance may change while the user enters the OTP.
             * Therefore, it must be checked again immediately before debit.
             */

            CheckBalanceRequestDto finalBalanceRequest =
                    new CheckBalanceRequestDto();

            finalBalanceRequest.setHierarchyId(childHierarchyId);
            finalBalanceRequest.setWalletType(walletType);
            finalBalanceRequest.setAmount(reverseAmount);

            try {

                checkBalance(finalBalanceRequest);

            } catch (RuntimeException e) {

                log.warn(
                        "Final wallet balance check failed. " +
                                "FundReversalId: {}, ChildHierarchyId: {}, Reason: {}",
                        fundReversal.getId(),
                        childHierarchyId,
                        e.getMessage()
                );

                return new CommonResponse<>(
                        400,
                        e.getMessage(),
                        null
                );
            }

            // ============================================================
            // STEP 10: PROCESS WALLET TRANSFER
            // ============================================================

            String baseTransactionId = commonMethods.generateBaseTransactionId();

            if (baseTransactionId == null || baseTransactionId.trim().isEmpty()) {

                throw new IllegalStateException("Unable to generate base transaction ID.");
            }

            log.info(
                    "Step 10: Processing wallet transfer. " +
                            "FundReversalId: {}, BaseTransactionId: {}, " +
                            "Amount: {}, WalletType: {}",
                    fundReversal.getId(),
                    baseTransactionId,
                    reverseAmount,
                    walletType
            );

            try {

                assert requestHeaderMetaDtoMeta != null;
                processWalletTransfer(
                        request,
                        reverseAmount,
                        walletType,
                        baseTransactionId,
                        requestHeaderMetaDtoMeta,
                        transactionDetailsInfo
                );

            } catch (Exception e) {

                log.error(
                        "Wallet transfer failed. " +
                                "FundReversalId: {}, BaseTransactionId: {}",
                        fundReversal.getId(),
                        baseTransactionId,
                        e
                );

                /*
                 * Throwing the exception is intentional.
                 * Because this method is @Transactional, Spring will roll
                 * back database changes made during wallet processing.
                 */
                throw new IllegalStateException(
                        "Wallet transfer failed.",
                        e
                );
            }

            log.info(
                    "Wallet transfer completed successfully. " +
                            "FundReversalId: {}, BaseTransactionId: {}",
                    fundReversal.getId(),
                    baseTransactionId
            );

            // ============================================================
            // STEP 11: PROCESS HIERARCHY CHARGES
            // ============================================================

            log.info(
                    "Step 11: Processing hierarchy charges. " +
                            "FundReversalId: {}, BaseTransactionId: {}",
                    fundReversal.getId(),
                    baseTransactionId
            );

            BigDecimal grossCharge;

            try {

                grossCharge =
                        calculateGrossCharge(
                                reverseAmount,
                                schemeRate
                        );

                if (grossCharge == null) {
                    grossCharge = BigDecimal.ZERO;
                }

                if (grossCharge.compareTo(BigDecimal.ZERO) < 0) {
                    throw new IllegalStateException(
                            "Calculated gross charge cannot be negative."
                    );
                }

                processHierarchyCharges(
                        request,
                        fundReversal,
                        walletType,
                        product,
                        subProduct,
                        schemeRate,
                        grossCharge,
                        baseTransactionId,
                        transactionDetailsInfo
                );

            } catch (Exception e) {

                log.error(
                        "Hierarchy charge processing failed. " +
                                "FundReversalId: {}, BaseTransactionId: {}",
                        fundReversal.getId(),
                        baseTransactionId,
                        e
                );

                /*
                 * Do not suppress this exception in production.
                 * Wallet transfer and hierarchy charges are one business
                 * transaction and should commit or roll back together.
                 */
                throw new IllegalStateException(
                        "Hierarchy charge processing failed.",
                        e
                );
            }

            log.info(
                    "Hierarchy charges processed successfully. " +
                            "FundReversalId: {}, BaseTransactionId: {}, GrossCharge: {}",
                    fundReversal.getId(),
                    baseTransactionId,
                    grossCharge
            );

            // ============================================================
            // STEP 12: MARK REVERSAL APPROVED
            // ============================================================

            fundReversal.setStatus(Status.APPROVED);
            fundReversal.setOtpVerified(true);
            fundReversal.setActionDate(LocalDate.now());
            fundReversal.setActionTime(LocalTime.now());

            /*
             * Clear OTP after successful use so it cannot be reused or
             * exposed later from the database.
             */
            fundReversal.setOtp(null);

            fundReversalRepository.save(fundReversal);

            FundReversalResponseDto responseDto =
                    new FundReversalResponseDto(
                            fundReversal.getId(),
                            null,
                            "TXN" + baseTransactionId
                    );

            long totalDuration =
                    System.currentTimeMillis() - startTime;

            log.info(
                    "===== Fund Reversal Completed Successfully ===== " +
                            "FundReversalId: {}, BaseTransactionId: {}, " +
                            "Amount: {}, WalletType: {}, DurationMs: {}",
                    fundReversal.getId(),
                    baseTransactionId,
                    reverseAmount,
                    walletType,
                    totalDuration
            );

            return new CommonResponse<>(
                    200,
                    "Transaction successful. Amount debited.",
                    responseDto
            );

        } catch (IllegalArgumentException e) {

            log.warn(
                    "Fund reversal validation failed. Reason: {}",
                    e.getMessage()
            );

            return new CommonResponse<>(
                    400,
                    e.getMessage(),
                    null
            );

        } catch (Exception e) {

            log.error(
                    "Unexpected error while processing fund reversal",
                    e
            );

            /*
             * Re-throwing is necessary for @Transactional rollback.
             * Handle this exception through your global exception handler.
             */
            throw new IllegalStateException(
                    "Fund reversal transaction failed. No changes were committed.",
                    e
            );
        }
    }

    private void processWalletTransfer(
            FundReversalRequestDto request,
            BigDecimal amount,
            WalletType walletType,
            String baseTransactionId,
            RequestHeaderMetaDto requestHeaderMetaDto,
            TransactionDetailsInfoDto transactionDetailsInfo) {

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
                transactionDetailsInfo.getSubProductCode(),
                "Payment",
                "Fund Reverse to " + request.getParentHierarchyId(),
                transactionDetailsInfo.getSchemeName(),
                retailerExtraColumns,
                transactionDetailsInfo.getVendorName(),
                transactionDetailsInfo.getOperatorName()
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
            WalletType walletType,
            TransactionDetailsInfoDto transactionDetailsInfoDto) {

        log.debug("Building transaction DTO for baseTransactionId: {}, hierarchyId: {}, walletType: {}",
                baseTransactionId, hierarchyId, walletType);

        TransactionSchemeDetailsRequestDto dto =
                new TransactionSchemeDetailsRequestDto();

        dto.setBaseTranxId(baseTransactionId);
        dto.setAmount(fundReversal.getReversalAmount().doubleValue());
        dto.setConfirmAmount(fundReversal.getReversalAmount().doubleValue());
        dto.setOperatorName(transactionDetailsInfoDto.getOperatorName());
        dto.setOperatorCode(schemeRate.getSchemeCode());
        dto.setSchemeRate(schemeRate);
        dto.setTransactionType(product.getTransactionType());
        dto.setWalletType(walletType.name());
        dto.setGstValue(String.valueOf(product.getSgst()));
        dto.setTdsRate(String.valueOf(product.getTds()));
        dto.setHierarchyId(hierarchyId);
        dto.setProductCode(product.getProductCode());
        dto.setSubProductCode(transactionDetailsInfoDto.getSubProductCode());
        dto.setIsCommission(product.getIsCommissions());
        dto.setHasCharge(product.getHasCharges());
        dto.setVendorName(transactionDetailsInfoDto.getVendorName());

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
            String baseTransactionId,
            TransactionDetailsInfoDto transactionDetailsInfoDto) {

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
                        parentId,
                        transactionDetailsInfoDto
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
            String hierarchyId,
            TransactionDetailsInfoDto transactionDetailsInfoDto) {

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
                        walletType,
                        transactionDetailsInfoDto
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
                        walletType,
                        transactionDetailsInfoDto
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
