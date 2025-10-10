package com.im.admin.service.impl;

import com.im.admin.commons.SMSGatewayAPI;
import com.im.admin.dto.AbstractWalletAmountTable;
import com.im.admin.dto.CheckBalanceRequestDto;
import com.im.admin.dto.FundReversalRequestDto;
import com.im.admin.entities.FundReversal;
import com.im.admin.entities.UserDetails;
import com.im.admin.entities.enums.Status;
import com.im.admin.entities.enums.WalletType;
import com.im.admin.repositories.FundReversalRepository;
import com.im.admin.repositories.UserDetailsRepository;
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

    @Override
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
            // Step 1: Generate OTP
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

           // Send OTP via SMS/Email
            Optional<UserDetails> HierarchyId = userDetailsRepository.findByHierarchyId(request.getChildHierarchyId());
            UserDetails userDetails = HierarchyId.get();

            smsGatewayAPI.sendOTPSms(userDetails.getMobileNumber(), otp);

            FundReversalResponseDto responseDto = new FundReversalResponseDto(
                    reversal.getId(),
                    otp,
                    null
            );

            return new CommonResponse<>(200, "OTP sent successfully. It will expire in 5 minutes.", responseDto);
        }

        // Step 2: Validate OTP
        Optional<FundReversal> optTxn = fundReversalRepository
                .findTopByParentHierarchyIdAndChildHierarchyIdAndStatusOrderByIdDesc(
                        request.getParentHierarchyId(), request.getChildHierarchyId(), Status.PENDING);

        if (optTxn.isEmpty()) {
            return new CommonResponse<>(404, "No pending transaction found.", null);
        }

        FundReversal fundReversal = optTxn.get();

        if (!fundReversal.getOtp().equals(request.getOtp())) {
            return new CommonResponse<>(400, "Invalid OTP.", null);
        }

        if (Duration.between(fundReversal.getOtpGeneratedTime(), LocalDateTime.now()).toMinutes() > 5) {
            return new CommonResponse<>(400, "OTP expired.", null);
        }

        // OTP Valid -> process transaction
        String baseTransactionId = commonMethods.generateBaseTransactionId();

        AbstractWalletAmountTable fromWalletTable =
                commonMethods.getWalletByType(request.getWalletType().name(), request.getChildHierarchyId());
        commonMethods.updateWallet(fromWalletTable, request.getChildHierarchyId(), request.getWalletType(),
                request.getReversalAmount(), "Debit", "TXN" + baseTransactionId, "SUCCESS", "DEBIT", "Fund Reversal");

        AbstractWalletAmountTable toWalletTable =
                commonMethods.getWalletByType(request.getWalletType().name(), request.getParentHierarchyId());
        commonMethods.updateWallet(toWalletTable, request.getParentHierarchyId(), request.getWalletType(),
                request.getReversalAmount(), "Credit", "TXN" + baseTransactionId, "SUCCESS", "CREDIT", "Fund Reversal");

        fundReversal.setStatus(Status.APPROVED);
        fundReversal.setOtpVerified(true);
        fundReversal.setActionDate(LocalDate.now());
        fundReversal.setActionTime(LocalTime.now());
        fundReversalRepository.save(fundReversal);

        FundReversalResponseDto responseDto = new FundReversalResponseDto(
                fundReversal.getId(),
                null,
                "TXN" + baseTransactionId
        );

        return new CommonResponse<>(200, "Transaction successful. Amount debited.", responseDto);
    }

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

        smsGatewayAPI.sendOTPSms(userDetails.getMobileNumber(), newOtp);

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

}

