package com.im.admin.service.impl;

import com.im.admin.commons.SMSGatewayAPI;
import com.im.admin.dto.AbstractWalletAmountTable;
import com.im.admin.dto.CheckBalanceRequestDto;
import com.im.admin.dto.FundReversalRequestDto;
import com.im.admin.dto.TransactionSchemeDetailsRequestDto;
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

    @Autowired
    private UserDetailsRepository userDetailsRepository;

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
    public CommonResponse<FundReversalResponseDto> takeActionOnFundReversal(FundReversalRequestDto request, Double latitude, Double longitude) {

        log.info("===== Starting Fund Reversal Action =====");
        log.info("Request: ParentHierarchyId={}, ChildHierarchyId={}, ReversalAmount={}, WalletType={}",
                request.getParentHierarchyId(),
                request.getChildHierarchyId(),
                request.getReversalAmount(),
                request.getWalletType());
        log.debug("Location - Latitude: {}, Longitude: {}", latitude, longitude);

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

            String otp = OtpUtil.generateOtp();
            log.debug("Generated OTP: {} for ChildHierarchyId: {}", otp, request.getChildHierarchyId());

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

        processWalletTransfer(request, reverseAmount, walletType, baseTransactionId, latitude, longitude);

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
            SubProducts subProduct = getSubProduct();
            Products product = subProduct.getProduct();
            log.debug("Product: {}, SubProduct: {}", product.getName(), subProduct.getSubProductTitle());

            SchemeRate schemeRate = walletToWalletTransferService
                    .getExactSchemeRate("116", reverseAmount.doubleValue());
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
            // Continue - the transaction is already successful, charges failure shouldn't rollback
            // But we should log it for investigation
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
            Double latitude, Double longitude) {

        AbstractWalletAmountTable fromWallet =
                commonMethods.getWalletByType(
                        walletType.name(),
                        request.getChildHierarchyId());

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
                latitude,
                longitude
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
                latitude,
                longitude
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

    private SubProducts getSubProduct() {

        return subProductsRepository
                .findBySubProductTitle("Fund Reversal")
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

        String currentId = request.getParentHierarchyId();

        while (true) {

            Optional<String> parentOpt =
                    commonMethods.findParentHierarchy(currentId);

            if (parentOpt.isEmpty()) {
                throw new RuntimeException(
                        "No approver found for hierarchy: " + currentId);
            }

            String parentId = parentOpt.get();
            String parentType =
                    commonMethods.extractHierarchyType(parentId);

            if ("WL".equals(parentType) ||
                    "IM".equals(parentType) ||
                    "AD".equals(parentType)) {

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

                break;
            }

            currentId = parentId;
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

        BigDecimal gstRate =
                BigDecimal.valueOf(product.getSgst())
                        .divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP);

        BigDecimal baseAmount =
                grossAmount.divide(BigDecimal.ONE.add(gstRate), 2, RoundingMode.HALF_UP);

        BigDecimal gstAmount =
                grossAmount.subtract(baseAmount).setScale(2, RoundingMode.HALF_UP);

        String parentHierarchyType =
                commonMethods.extractHierarchyType(request.getParentHierarchyId());

        String parentTable =
                transactionEngineServiceImpl.getTransactionTable(parentHierarchyType, walletType.name());

        AbstractWalletAmountTable parentWallet =
                commonMethods.getWalletByType(walletType.name(), request.getParentHierarchyId());

        BigDecimal parentBalance =
                parentWallet.getAvailableBalance().setScale(2, RoundingMode.HALF_UP);

        /* CHARGE DEBIT */

        BigDecimal afterChargeDebit =
                parentBalance.subtract(baseAmount).setScale(2, RoundingMode.HALF_UP);

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

        /* GST DEBIT */

        BigDecimal afterGstDebit =
                afterChargeDebit.subtract(gstAmount).setScale(2, RoundingMode.HALF_UP);

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


        /* ================= WL / ADMIN HIERARCHY (CREDIT) ================= */

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

        AbstractWalletAmountTable wallet =
                commonMethods.getWalletByType(walletType.name(), hierarchyId);

        BigDecimal walletBalance =
                wallet.getAvailableBalance().setScale(2, RoundingMode.HALF_UP);

        /* CHARGE CREDIT */

        BigDecimal afterChargeCredit =
                walletBalance.add(baseAmount).setScale(2, RoundingMode.HALF_UP);

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

        /* GST CREDIT */

        BigDecimal afterGstCredit =
                afterChargeCredit.add(gstAmount).setScale(2, RoundingMode.HALF_UP);

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
    }

}
