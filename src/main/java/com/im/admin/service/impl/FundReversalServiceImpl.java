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
import java.util.List;
import java.util.Map;
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
    public CommonResponse<FundReversalResponseDto> takeActionOnFundReversal(FundReversalRequestDto request) {

        try {
            CheckBalanceRequestDto balanceRequest = new CheckBalanceRequestDto();
            balanceRequest.setHierarchyId(request.getChildHierarchyId());
            balanceRequest.setWalletType(request.getWalletType());
            balanceRequest.setAmount(request.getReversalAmount());

            checkBalance(balanceRequest);

        } catch (RuntimeException e) {
            return new CommonResponse<>(400, e.getMessage(), null);
        }

        if (request.getOtp() == null || request.getOtp().isEmpty()) {

            String otp = OtpUtil.generateOtp();

            FundReversal reversal = new FundReversal();
            reversal.setParentHierarchyId(request.getParentHierarchyId());
            reversal.setChildHierarchyId(request.getChildHierarchyId());
            reversal.setReversalAmount(request.getReversalAmount());
            reversal.setWalletType(request.getWalletType());
            reversal.setOtp(otp);
            reversal.setOtpGeneratedTime(LocalDateTime.now());
            reversal.setStatus(Status.PENDING);

            fundReversalRepository.save(reversal);

            Optional<UserDetails> userOpt =
                    userDetailsRepository.findByHierarchyId(request.getChildHierarchyId());

            FundReversalResponseDto responseDto =
                    new FundReversalResponseDto(reversal.getId(), otp, null);

            return new CommonResponse<>(200,
                    "OTP sent successfully. It will expire in 5 minutes.",
                    responseDto);
        }

        Optional<FundReversal> optTxn =
                fundReversalRepository
                        .findTopByParentHierarchyIdAndChildHierarchyIdAndStatusOrderByIdDesc(
                                request.getParentHierarchyId(),
                                request.getChildHierarchyId(),
                                Status.PENDING);

        if (optTxn.isEmpty()) {
            return new CommonResponse<>(404, "No pending transaction found.", null);
        }

        FundReversal fundReversal = optTxn.get();

        if (!fundReversal.getOtp().equals(request.getOtp())) {
            return new CommonResponse<>(400, "Invalid OTP.", null);
        }

        if (Duration.between(
                fundReversal.getOtpGeneratedTime(),
                LocalDateTime.now()).toMinutes() > 5) {

            return new CommonResponse<>(400, "OTP expired.", null);
        }

        BigDecimal reverseAmount = fundReversal.getReversalAmount();
        WalletType walletType = fundReversal.getWalletType();

        String baseTransactionId = commonMethods.generateBaseTransactionId();

        processWalletTransfer(request, reverseAmount, walletType, baseTransactionId);

        fundReversal.setStatus(Status.APPROVED);
        fundReversal.setOtpVerified(true);
        fundReversal.setActionDate(LocalDate.now());
        fundReversal.setActionTime(LocalTime.now());

        fundReversalRepository.save(fundReversal);

        SubProducts subProduct = getSubProduct(request.getSubProductId());
        Products product = subProduct.getProduct();

        SchemeRate schemeRate =
                walletToWalletTransferService
                        .getExactSchemeRate("66", reverseAmount.doubleValue());

        BigDecimal grossCharge =
                calculateGrossCharge(reverseAmount, schemeRate);

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

        FundReversalResponseDto responseDto =
                new FundReversalResponseDto(
                        fundReversal.getId(),
                        null,
                        "TXN" + baseTransactionId
                );

        return new CommonResponse<>(200,
                "Transaction successful. Amount debited.",
                responseDto);
    }

    private void processWalletTransfer(
            FundReversalRequestDto request,
            BigDecimal amount,
            WalletType walletType,
            String baseTransactionId) {

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
                ""
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
                ""
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

    private SubProducts getSubProduct(String subProductId) {

        return subProductsRepository
                .findById(subProductId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "SubProduct not found for subProductId: "
                                        + subProductId));
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
