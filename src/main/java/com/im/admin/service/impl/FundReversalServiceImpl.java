package com.im.admin.service.impl;

import com.im.admin.dto.AbstractWalletAmountTable;
import com.im.admin.dto.CheckBalanceRequestDto;
import com.im.admin.dto.FundReversalRequestDto;
import com.im.admin.entities.FundReversal;
import com.im.admin.entities.enums.Status;
import com.im.admin.entities.enums.WalletType;
import com.im.admin.repositories.FundReversalRepository;
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
    private FundReversalRepository fundReversalRepository;

    @Autowired
    private CommonMethods commonMethods;

    @Override
    public String takeActionOnFundReversal(FundReversalRequestDto request) {

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

            fundReversalRepository.save(reversal);

            // TODO: Send OTP to child via SMS/Email

            return "OTP sent successfully. It will expire in 5 minutes. OTP: " + otp + ";";
        } else {
            // Step 2: Validate OTP
            Optional<FundReversal> optTxn = fundReversalRepository
                    .findTopByParentHierarchyIdAndChildHierarchyIdAndStatusOrderByIdDesc(
                            request.getParentHierarchyId(), request.getChildHierarchyId(), Status.PENDING);

            if (optTxn.isEmpty()) return "No pending transaction found.";
            FundReversal fundReversal = optTxn.get();

            if (!fundReversal.getOtp().equals(request.getOtp())) {
                return "Invalid OTP.";
            }

            if (Duration.between(fundReversal.getOtpGeneratedTime(), LocalDateTime.now()).toMinutes() > 5) {
                return "OTP expired.";
            }

            String baseTransactionId = commonMethods.generateBaseTransactionId();

            AbstractWalletAmountTable fromWalletTable = commonMethods.getWalletByType(request.getWalletType().name(), request.getChildHierarchyId());
            commonMethods.updateWallet(fromWalletTable, request.getChildHierarchyId(), request.getWalletType(), request.getReversalAmount(), "Debit", "TXN"+baseTransactionId, "SUCCESS","DEBIT");

            AbstractWalletAmountTable toWalletTable = commonMethods.getWalletByType(request.getWalletType().name(), request.getParentHierarchyId());
            commonMethods.updateWallet(toWalletTable, request.getParentHierarchyId(), request.getWalletType(), request.getReversalAmount(), "Credit", "TXN"+baseTransactionId, "SUCCESS","CREDIT");

            fundReversal.setStatus(Status.APPROVED);
            fundReversal.setOtpVerified(true);
            fundReversal.setActionDate(LocalDate.now());
            fundReversal.setActionTime(LocalTime.now());
            fundReversalRepository.save(fundReversal);

            return "Transaction successful. Amount debited.";
        }
    }

    @Override
    public String resendOtpById(Long id) {
        Optional<FundReversal> optReversal = fundReversalRepository.findById(id);

        if (optReversal.isEmpty()) {
            return "No fund reversal request found with the given ID.";
        }

        FundReversal fundReversal = optReversal.get();

//        if (fundReversal.getStatus() != ConstantStatus.PENDING) {
//            return "Cannot resend OTP. Transaction is already " + fundReversal.getStatus().name() + ".";
//        }

        if (Boolean.TRUE.equals(fundReversal.getOtpVerified())) {
            return "OTP already verified. Cannot resend.";
        }

        long minutesSinceGenerated = Duration.between(fundReversal.getOtpGeneratedTime(), LocalDateTime.now()).toMinutes();
        if (minutesSinceGenerated < 5) {
            return "Current OTP is still valid. Please wait until it expires.";
        }

        // Generate new OTP and update the record
        String newOtp = OtpUtil.generateOtp();
        fundReversal.setOtp(newOtp);
        fundReversal.setOtpGeneratedTime(LocalDateTime.now());
        fundReversalRepository.save(fundReversal);

        // TODO: Send new OTP to child via SMS/Email

        return "New OTP sent successfully. It will expire in 5 minutes. OTP: " + newOtp + ";";
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

