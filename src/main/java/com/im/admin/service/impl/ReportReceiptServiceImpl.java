package com.im.admin.service.impl;

import com.im.admin.entities.*;
import com.im.admin.repositories.*;
import com.im.admin.request.ReportReceiptRequestDto;
import com.im.admin.response.AepsReceiptResponseDto;
import com.im.admin.response.DmtReceiptResponseDto;
import com.im.admin.service.ReportReceiptService;
import com.im.admin.util.AbstractRetailerTransactionDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;


@Service
public class ReportReceiptServiceImpl implements ReportReceiptService {

    @Autowired
    private RetailerPrepaidTransactionDetailsRepository retailerPrepaidTransactionDetailsRepository;

    @Autowired
    private RetailerPostpaidTransactionDetailsRepository retailerPostpaidTransactionDetailsRepository;

    @Autowired
    private SubProductsRepository subProductsRepository;

    @Autowired
    private DmtTransactionRepository dmtTransactionRepository;

    @Autowired
    private AepsBalanceEnquiryRepository aepsBalanceEnquiryRepository;

    @Autowired
    private AepsMiniStatementRepository aepsMiniStatementRepository;

    @Autowired
    private AepsCashWithdrawalRepository aepsCashWithdrawalRepository;


    @Override
    public Object getReceipt(ReportReceiptRequestDto request) {

        AbstractRetailerTransactionDetails txn;

        //  Step 1: Fetch transaction
        if ("PREPAID".equalsIgnoreCase(request.getWalletType())) {

            txn = retailerPrepaidTransactionDetailsRepository.findByTransactionId(request.getTxnId())
                    .orElseThrow(() -> new RuntimeException("Transaction not found"));

        } else {

            txn = retailerPostpaidTransactionDetailsRepository.findByTransactionId(request.getTxnId())
                    .orElseThrow(() -> new RuntimeException("Transaction not found"));
        }

        //  Step 2: Get SubProduct
        SubProducts sp =
                subProductsRepository.findBySubProductCode(txn.getSubProductName())
                        .orElseThrow(() -> new RuntimeException("SubProduct not found"));

        String productName = sp.getProduct().getName();


        // ======================= DMT =============================

        if (productName != null &&
                productName.toLowerCase().contains("dmt")) {

            DmtTransaction dmt =
                    dmtTransactionRepository.findByOrderId(request.getTxnId())
                            .orElseThrow(() -> new RuntimeException("DMT record not found"));

            return DmtReceiptResponseDto.builder()
                    .status(dmt.getVendorStatus())
                    .dateTime(dmt.getCreatedAt().toString())
                    .txnType(dmt.getTransactionType())
                    .beneficiaryName(dmt.getBeneName())
                    .amount(Double.valueOf(dmt.getTxnAmount()))
                    .orderId(dmt.getOrderId())
                    .vendorTxnId(dmt.getAckno())
                    .utr(dmt.getUtr())
                    .referenceId(dmt.getReferenceId())
                    .message(dmt.getMessage())
                    .remarks(dmt.getRemarks())
                    .build();
        }


        // ======================= AEPS ============================

        if (productName != null &&
                productName.toLowerCase().contains("aeps")) {

            String operator = txn.getOperatorName();

            if ("BE".equalsIgnoreCase(operator)) {

                AepsBalanceEnquiry be =
                        aepsBalanceEnquiryRepository.findByTransactionId(request.getTxnId())
                                .orElseThrow(() -> new RuntimeException("BE record not found"));

                return mapAepsResponse(
                        "Balance Enquiry",
                        "balanceEnquiry",
                        be.getTransactionStatus(),
                        be.getTransactionId(),
                        be.getAadhaarNumber(),
                        be.getMobileNumber(),
                        be.getBankName(),
                        be.getTransactionAmount(),
                        be.getBalanceAmount(),
                        be.getBankRrn(),
                        be.getTerminalId(),
                        be.getStatusMessage(),
                        be.getCreatedAt() != null ? be.getCreatedAt().toString() : null

                );
            }

            if ("MS".equalsIgnoreCase(operator)) {

                AepsMiniStatement ms =
                        aepsMiniStatementRepository.findByTransactionId(request.getTxnId())
                                .orElseThrow(() -> new RuntimeException("MS record not found"));

                return mapAepsResponse(
                        "Mini Statement",
                        "miniStatement",
                        ms.getTransactionStatus(),
                        ms.getTransactionId(),
                        ms.getAadhaarNumber(),
                        ms.getMobileNumber(),
                        ms.getBankName(),
                        ms.getTransactionAmount(),
                        ms.getBalanceAmount(),
                        ms.getBankRrn(),
                        ms.getTerminalId(),
                        ms.getStatusMessage(),
                        ms.getCreatedAt() != null ? ms.getCreatedAt().toString() : null
                );
            }

            if ("CW".equalsIgnoreCase(operator)) {

                AepsCashWithdrawalLog cw =
                        aepsCashWithdrawalRepository.findByTransactionId(request.getTxnId())
                                .orElseThrow(() -> new RuntimeException("CW record not found"));

                return mapAepsResponse(
                        "Cash Withdrawal",
                        "cashWithdrawal",
                        cw.getTransactionStatus(),
                        cw.getTransactionId(),
                        cw.getAadhaarNumber(),
                        cw.getMobileNumber(),
                        cw.getBankName(),
                        cw.getTransactionAmount(),
                        cw.getBalanceAmount(),
                        cw.getBankRrn(),
                        cw.getTerminalId(),
                        cw.getStatusMessage(),
                        cw.getCreatedAt() != null ? cw.getCreatedAt().toString() : null
                );
            }
        }

        throw new RuntimeException("Unsupported Product");
    }


    // ================= Common AEPS Mapper ===================
    private AepsReceiptResponseDto mapAepsResponse(
            String title,
            String key,
            String status,
            String txnId,
            String aadhaar,
            String mobile,
            String bank,
            Double amount,
            Double balance,
            String rrn,
            String terminalId,
            String statusMessage,
             String dateTime) {

        return AepsReceiptResponseDto.builder()
                .serviceTitle(title)
                .serviceKey(key)
                .status(status)
                .transactionId(txnId)
                .aadhaarNumber(aadhaar)
                .mobileNumber(mobile)
                .bankName(bank)
                .transactionAmount(amount)
                .balanceAmount(balance)
                .rrn(rrn)
                .terminalId(terminalId)
                .errorMessage(null)
                .statusMessage(statusMessage)
                .dateTime(dateTime)
                .build();
    }
}