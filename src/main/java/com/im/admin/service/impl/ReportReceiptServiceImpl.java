package com.im.admin.service.impl;

import com.im.admin.entities.*;
import com.im.admin.repositories.*;
import com.im.admin.request.ReportReceiptRequestDto;
import com.im.admin.response.*;
import com.im.admin.service.ReportReceiptService;
import com.im.admin.util.AbstractRetailerTransactionDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;


@Service
public class ReportReceiptServiceImpl implements ReportReceiptService {

    @Autowired
    private BeneficiaryRepository beneficiaryRepository;

    @Autowired
    private RetailerPrepaidTransactionDetailsRepository retailerPrepaidTransactionDetailsRepository;

    @Autowired
    private RetailerPostpaidTransactionDetailsRepository retailerPostpaidTransactionDetailsRepository;

    @Autowired
    private SubProductsRepository subProductsRepository;

    @Autowired
    private PrefundRequestRepository prefundRequestRepository;

    @Autowired
    private DmtTransactionRepository dmtTransactionRepository;

    @Autowired
    private AepsBalanceEnquiryRepository aepsBalanceEnquiryRepository;

    @Autowired
    private AepsMiniStatementRepository aepsMiniStatementRepository;

    @Autowired
    private AepsCashWithdrawalRepository aepsCashWithdrawalRepository;

    @Autowired
    private BillAvenueTransactionRepository billAvenueTransactionRepository;

    @Autowired
    private BeneficiaryBankAccountRepository beneficiaryBankAccountRepository;

    @Autowired
    private RechargeTransactionRepository rechargeTransactionRepository;

    @Autowired
    private RechargeOperatorsRepository rechargeOperatorsRepository;


    @Autowired
    PgTransactionMappingRepository pgTransactionMappingRepository;

    @Autowired
    private CallbackEventLogRepository callbackEventLogRepository;

    @Autowired
    private CardTypeMasterRepository cardTypeMasterRepository;

    @Autowired
    PayoutPendingTransactionRepository payoutPendingTransactionRepository;


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
//        SubProducts sp =
//                subProductsRepository.findBySubProductCode(txn.getSubProductName())
//                        .orElseThrow(() -> new RuntimeException("SubProduct not found"));

        SubProducts sp = subProductsRepository
                .findBySubProductCodeOrSubProductTitle(
                        txn.getSubProductName(),
                        txn.getSubProductName()
                )
                .orElseThrow(() -> new RuntimeException("SubProduct not found"));

        String productName = sp.getProduct().getName();


        // ======================= Recharge =============================//

        if (productName != null &&
                productName.toLowerCase().contains("recharge")) {

            RechargeTransaction rechargeTransaction =
                    rechargeTransactionRepository.findByTransactionRef(request.getTxnId())
                            .orElseThrow(() -> new RuntimeException("Recharge record not found"));

            String operatorName = rechargeOperatorsRepository
                    .findByCodeAndType(rechargeTransaction.getOperatorCode(), "MOBILE")
                    .map(RechargeOperators::getName)
                    .orElseThrow(() -> new RuntimeException(
                            "Operator not found for code: " + rechargeTransaction.getOperatorCode() + " and type MOBILE"
                    ));
            return RechargeReceiptResponseDto.builder()
                    .status(rechargeTransaction.getStatus())
                    .amount(rechargeTransaction.getAmount())
                    .tranxId(rechargeTransaction.getTransactionId())
                    .operatorName(operatorName)
                    .dateTime(rechargeTransaction.getCreatedAt().toString())
                    .customerNumber(rechargeTransaction.getCustomerNumber())
                    .vendorResp(rechargeTransaction.getVendorResponse())
                    .build();

        }

        // ======================= DMT =============================

        if (productName != null &&
                productName.toLowerCase().contains("dmt")) {

            DmtTransaction dmt =
                    dmtTransactionRepository.findByOrderId(request.getTxnId())
                            .orElseThrow(() -> new RuntimeException("DMT record not found"));

            // Get beneficiary using account number
            DmtBeneficiary beneficiary =
                    beneficiaryRepository.findByAccountNo(dmt.getAccountNumber())
                            .orElse(null);

            // Get IFSC from beneficiary table
            String ifscCode = beneficiary != null
                    ? beneficiary.getIfscCode()
                    : null;

            return DmtReceiptResponseDto.builder()
                    .status(dmt.getVendorStatus())
                    .dateTime(dmt.getCreatedAt().toString())
                    .txnType(dmt.getTransactionType())
                    .beneficiaryName(dmt.getBeneName())
                    .accNumber(dmt.getAccountNumber())
                    .ifsc(ifscCode)
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

        // ======================= BBPS ============================

        if (productName != null &&
                productName.toLowerCase().contains("bbps")) {

            BillAvenueTransaction bbps =
                    billAvenueTransactionRepository
                            .findByTxnId(request.getTxnId())
                            .orElseThrow(() ->
                                    new RuntimeException("BBPS record not found"));

            return BbpsReceiptResponseDto.builder()
                    .createdDateTime(
                            bbps.getCreatedDateTime() != null
                                    ? bbps.getCreatedDateTime().toString()
                                    : null
                    )
                    .txnStatus(bbps.getTxnStatus())
                    .billerName(bbps.getBillerName())
                    .txnAmount(bbps.getTxnAmount())
                    .customerName(bbps.getCustomerName())
                    .consumerNumber(bbps.getConsumerNumber())
                    .category(bbps.getCategory())
                    .txnId(bbps.getTxnId())
                    .referenceNumber(bbps.getReferenceNumber())
                    .build();
        }

//        // ======================= PG ============================
//
//        if (productName != null &&
//           hie     productName.trim().equalsIgnoreCase("payment gateway")) {
//
//            // Step 1: Get mapping
////            PgTransactionMapping pg =
////                    pgTransactionMappingRepository
////                            .findByTxnId(request.getTxnId())
////                            .orElseThrow(() ->
////                                    new RuntimeException("PG mapping not found"));
//
//            String txnId = request.getTxnId();
//
//            String orderIdPart = txnId.replace("TXN", "")
//                    .replace("FLD", "");
//
//            PgTransactionMapping pg =
//                    pgTransactionMappingRepository
//                            .findByOrderIdLike(orderIdPart)
//                            .orElseThrow(() ->
//                                    new RuntimeException("PG mapping not found"));
//
//            String orderId = pg.getOrderId();
//            String cardBin = pg.getCardBinNo();
//
//            // Step 2: Get callback log
////            String txnPart = orderId.replace("ORD", "");
//
////            CallbackEventLog callback =
////                    callbackEventLogRepository
////                            .findLatestPgCallback(txnPart)
////                            .orElseThrow(() ->
////                                    new RuntimeException("Callback log not found"));
//
//            CallbackEventLog callback =
//                    callbackEventLogRepository
//                            .findLatestPgCallback(orderId)
//                            .orElseThrow(() ->
//                                    new RuntimeException("Callback log not found"));
//
//            Map<String, String> parsedRequest = parseRequest(callback.getRequest());
//
//            String status = parsedRequest.get("STATUS");
//            String txnAmount = parsedRequest.get("TXN_AMOUNT");
//            String txnDate = parsedRequest.get("TXN_DATE");
//            String paymentMode = parsedRequest.get("CARD_CATEGORY");
//            String phone = parsedRequest.get("CUSTOMER_PHONE");
//            String bankRefNo = parsedRequest.get("BANK_TXNID");
//
//            // Step 3: Get bank name from card_type_master
//            String bankName = cardTypeMasterRepository
//                    .findByCardBinNo(cardBin)
//                    .map(CardTypeMaster::getIssuerName)
//                    .orElse("Unknown");
//
//            return PgReceiptResponseDto.builder()
//                    .dateTime(txnDate.replace(" ", "T"))
//                    .txnId(request.getTxnId())
//                    .orderId(orderId)
//                    .number(phone)
//                    .txnAmount(Long.parseLong(txnAmount))
//                    .paymentMode(paymentMode)
//                    .bankName(bankName)
//                    .bankRefNo(bankRefNo)
//                    .txnStatus(status)
//                    .build();
//        }

        // ======================= PG ============================

        if (productName != null &&
                productName.trim().equalsIgnoreCase("payment gateway")) {

            // Step 1 : Get PG Mapping
            String txnId = request.getTxnId();

            String orderIdPart = txnId.replace("TXN", "")
                    .replace("FLD", "");

            PgTransactionMapping pg =
                    pgTransactionMappingRepository
                            .findByOrderIdLike(orderIdPart)
                            .orElseThrow(() ->
                                    new RuntimeException("PG mapping not found"));

            String orderId = pg.getOrderId();
            String cardBin = pg.getCardBinNo();

            // Step 2 : Get transaction details from already fetched txn
            String status = txn.getTranStatus();

            BigDecimal txnAmount = txn.getAmount();

            LocalDate date = txn.getDateOfTransaction();
            LocalTime time = txn.getTimeOfTransaction();

            String txnDate = LocalDateTime.of(date, time).toString();

            String paymentMode = txn.getOperatorName();

            String phone = txn.getMobileNo();

            String bankRefNo = txn.getBankRefId();

            // Step 3 : Get bank name
            String bankName = cardTypeMasterRepository
                    .findByCardBinNo(cardBin)
                    .map(CardTypeMaster::getIssuerName)
                    .orElse("Unknown");

            return PgReceiptResponseDto.builder()
                    .dateTime(txnDate)
                    .txnId(request.getTxnId())
                    .orderId(orderId)
                    .number(phone)
                    .txnAmount(txnAmount.longValue())
                    .paymentMode(paymentMode)
                    .bankName(bankName)
                    .bankRefNo(bankRefNo)
                    .txnStatus(status)
                    .build();
        }

//        // ======================= PAYOUT ============================
//
//        if (productName != null &&
//                productName.trim().equalsIgnoreCase("payout")) {
//
//            // Step 1: Get payout record
//            PayoutPendingTransactions payout =
//                    payoutPendingTransactionRepository
//                            .findByTransactionId(request.getTxnId())
//                            .orElseThrow(() ->
//                                    new RuntimeException("Payout record not found"));
//
//            // Step 2: Get callback
//
////            CallbackEventLog callback =
////                    callbackEventLogRepository
////                            .findLatestByTxnIdInRequest(payout.getTransactionId())
////                            .orElseThrow(() ->
////                                    new RuntimeException("Callback log not found"));
//
//            CallbackEventLog callback =
//                    callbackEventLogRepository
//                            .findLatestByTxnIdInRequest(payout.getTransactionId())
//                            .orElse(null);
//
//            if (callback == null
//                    && payout.getTempTxnId() != null
//                    && !payout.getTempTxnId().isBlank()) {
//
//                callback = callbackEventLogRepository
//                        .findLatestByTxRef(payout.getTempTxnId())
//                        .orElse(null);
//            }
//
//            if (callback == null) {
//                throw new RuntimeException("Callback log not found");
//            }
//
//            BeneficiaryBankAccount bankAccount =
//                    beneficiaryBankAccountRepository
//                            .findTopByRetailerIdAndAccountNumberOrderByIdDesc(
//                                    payout.getRetailerId(),
//                                    payout.getAccountNumber()
//                            )
//                            .orElse(null);
//
//            String bankName = bankAccount != null
//                    ? bankAccount.getBankName()
//                    : "Unknown";
//
//            Map<String, String> parsed = parseRequest(callback.getRequest());
//
//            String status = parsed.containsKey("STATUS")
//                    ? parsed.get("STATUS")
//                    : parsed.containsKey("txnStatus")
//                    ? parsed.get("txnStatus")
//                    : parsed.get("status");
//
//            String txnMode = parsed.containsKey("TXN_MODE")
//                    ? parsed.get("TXN_MODE")
//                    : parsed.containsKey("paymentMode")
//                    ? parsed.get("paymentMode")
//                    : parsed.get("operator");
//
//            String txnAmount = parsed.containsKey("TXN_AMOUNT")
//                    ? parsed.get("TXN_AMOUNT")
//                    : parsed.containsKey("amount")
//                    ? parsed.get("amount")
//                    : payout.getAmount().toPlainString();
//
//            String utr = parsed.containsKey("UTRNO")
//                    ? parsed.get("UTRNO")
//                    : parsed.get("rrn");
//
//            String processedTime = parsed.containsKey("PROCESSED_TIME")
//                    ? parsed.get("PROCESSED_TIME")
//                    : null;
//
//            return PayoutReceiptResponseDto.builder()
//                    .txnId(payout.getTransactionId())
//                    .txnStatus(status)
//                    .txnAmount(txnAmount != null ? Long.parseLong(txnAmount) : 0L)
//                    .dateTime(processedTime != null
//                            ? processedTime.replace(" ", "T")
//                            : payout.getCreatedAt().toString())
//                    .accountNumber(payout.getAccountNumber())
//                    .ifscCode(payout.getIfscCode())
//                    .bankName(bankName)
//                    .accountName(
//                            bankAccount != null
//                                    ? bankAccount.getBankHolderName()
//                                    : payout.getBeneficiaryName()
//                    )
//                    .txnMode(txnMode)
//                    .utrNumber(utr)
//                    .guid(payout.getRetailerId())
//                    .build();
//        }
//
//        throw new RuntimeException("Unsupported Product");
//    }

        // ======================= PAYOUT ============================

        if (productName != null &&
                productName.trim().equalsIgnoreCase("payout")) {

            // Step 1: Get payout record
            PayoutPendingTransactions payout =
                    payoutPendingTransactionRepository
                            .findByTransactionId(request.getTxnId())
                            .orElseThrow(() ->
                                    new RuntimeException("Payout record not found"));

            // Step 2: Get beneficiary bank details
            BeneficiaryBankAccount bankAccount =
                    beneficiaryBankAccountRepository
                            .findTopByRetailerIdAndAccountNumberOrderByIdDesc(
                                    payout.getRetailerId(),
                                    payout.getAccountNumber()
                            )
                            .orElse(null);

            String bankName = bankAccount != null
                    ? bankAccount.getBankName()
                    : "Unknown";

            // Step 3: Get details from already fetched transaction table
            String status = txn.getTranStatus();

            BigDecimal txnAmount = txn.getAmount();

            String txnMode = txn.getOperatorName();

          String utrno=   txn.getBankRefId();

            LocalDate date = txn.getDateOfTransaction();
            LocalTime time = txn.getTimeOfTransaction();

            String dateTime = LocalDateTime.of(date, time).toString();

            String utr = utrno;

            return PayoutReceiptResponseDto.builder()
                    .txnId(payout.getTransactionId())
                    .txnStatus(status)
                    .txnAmount(txnAmount != null
                            ? txnAmount.longValue()
                            : 0L)
                    .dateTime(dateTime)
                    .accountNumber(payout.getAccountNumber())
                    .ifscCode(payout.getIfscCode())
                    .bankName(bankName)
                    .accountName(
                            bankAccount != null
                                    ? bankAccount.getBankHolderName()
                                    : payout.getBeneficiaryName()
                    )
                    .txnMode(txnMode)
                    .utrNumber(utr)
                    .guid(payout.getRetailerId())
                    .build();
        }

        // ======================= MTB ============================

        if (productName != null &&
                productName.trim().equalsIgnoreCase("Move To Bank")) {

            // Step 1: Get payout record
            PayoutPendingTransactions payout =
                    payoutPendingTransactionRepository
                            .findByTransactionId(request.getTxnId())
                            .orElseThrow(() ->
                                    new RuntimeException("Payout record not found"));

            // Step 2: Get beneficiary bank details
            BeneficiaryBankAccount bankAccount =
                    beneficiaryBankAccountRepository
                            .findTopByRetailerIdAndAccountNumberOrderByIdDesc(
                                    payout.getRetailerId(),
                                    payout.getAccountNumber()
                            )
                            .orElse(null);

            String bankName = bankAccount != null
                    ? bankAccount.getBankName()
                    : "Unknown";

            // Step 3: Get details from already fetched transaction table
            String status = txn.getTranStatus();

            BigDecimal txnAmount = txn.getAmount();

            String txnMode = txn.getOperatorName();

            LocalDate date = txn.getDateOfTransaction();
            LocalTime time = txn.getTimeOfTransaction();

            String dateTime = LocalDateTime.of(date, time).toString();

//
            String utr = txn.getBankRefId();

            return PayoutReceiptResponseDto.builder()
                    .txnId(payout.getTransactionId())
                    .txnStatus(status)
                    .txnAmount(txnAmount != null
                            ? txnAmount.longValue()
                            : 0L)
                    .dateTime(dateTime)
                    .accountNumber(payout.getAccountNumber())
                    .ifscCode(payout.getIfscCode())
                    .bankName(bankName)
                    .accountName(
                            bankAccount != null
                                    ? bankAccount.getBankHolderName()
                                    : payout.getBeneficiaryName()
                    )
                    .txnMode(txnMode)
                    .utrNumber(utr)
                    .guid(payout.getRetailerId())
                    .build();
        }

        // ======================= BALANCE TRANSFER ============================
        if (productName != null &&
                productName.trim().equalsIgnoreCase("balance transfer")) {

            String paymentMode = txn.getOperatorName();

            return BalanceTransferReceiptResponseDto.builder()
                    .dateTime(
                            txn.getDateOfTransaction() != null &&
                                    txn.getTimeOfTransaction() != null
                                    ? LocalDateTime.of(
                                    txn.getDateOfTransaction(),
                                    txn.getTimeOfTransaction()
                            ).toString()
                                    : null
                    )
                    .txnId(request.getTxnId())
                    .amount(txn.getAmount())
                    .remarks(txn.getRemarks())
                    .status(txn.getTranStatus())
                    .paymentMode(paymentMode)
                    .build();
        }

        // ======================= Fund Reversal ============================
        if (productName != null &&
                productName.trim().equalsIgnoreCase("Fund Reversal")) {

            String paymentMode = txn.getOperatorName();

            return BalanceTransferReceiptResponseDto.builder()
                    .dateTime(
                            txn.getDateOfTransaction() != null &&
                                    txn.getTimeOfTransaction() != null
                                    ? LocalDateTime.of(
                                    txn.getDateOfTransaction(),
                                    txn.getTimeOfTransaction()
                            ).toString()
                                    : null
                    )
                    .txnId(request.getTxnId())
                    .amount(txn.getAmount())
                    .remarks(txn.getRemarks())
                    .status(txn.getTranStatus())
                    .paymentMode(paymentMode)
                    .build();
        }

        // ======================= Fund Transfer ============================
        if (productName != null &&
                productName.trim().equalsIgnoreCase("Fund Transfer")) {

            String paymentMode = txn.getOperatorName();

            return BalanceTransferReceiptResponseDto.builder()
                    .dateTime(
                            txn.getDateOfTransaction() != null &&
                                    txn.getTimeOfTransaction() != null
                                    ? LocalDateTime.of(
                                    txn.getDateOfTransaction(),
                                    txn.getTimeOfTransaction()
                            ).toString()
                                    : null
                    )
                    .txnId(request.getTxnId())
                    .amount(txn.getAmount())
                    .remarks(txn.getRemarks())
                    .status(txn.getTranStatus())
                    .paymentMode(paymentMode)
                    .build();
        }
        // ======================= Fund Request ============================
        if (productName != null &&
                productName.trim().equalsIgnoreCase("Fund Request")) {

            String paymentMode = txn.getOperatorName();

            PrefundRequest fundRequest = prefundRequestRepository
                    .findByAdminTranId(request.getTxnId())
                    .orElse(null);

            return BalanceTransferReceiptResponseDto.builder()
                    .dateTime(
                            txn.getDateOfTransaction() != null &&
                                    txn.getTimeOfTransaction() != null
                                    ? LocalDateTime.of(
                                    txn.getDateOfTransaction(),
                                    txn.getTimeOfTransaction()
                            ).toString()
                                    : null
                    )
                    .txnId(request.getTxnId())
                    .amount(txn.getAmount())
                    .remarks(txn.getRemarks())
                    .status(txn.getTranStatus())
                    .paymentMode(paymentMode)

                    .requestDate(
                            fundRequest != null && fundRequest.getCreateDateTime() != null
                                    ? fundRequest.getCreateDateTime().toString()
                                    : null
                    )
                    .depositDate(
                            fundRequest != null && fundRequest.getDepositDate() != null
                                    ? fundRequest.getDepositDate().toString()
                                    : null
                    )
                    .approveOrRejectDate(
                            fundRequest != null && fundRequest.getApproveOrRejectDate() != null
                                    ? fundRequest.getApproveOrRejectDate().toString()
                                    : null
                    )
                    .bankAccountName(
                            fundRequest != null
                                    ? fundRequest.getBankAccountName()
                                    : null
                    )
                    .bankAccountNumber(
                            fundRequest != null
                                    ? fundRequest.getBankAccountNumber()
                                    : null
                    )
                    .build();
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


    private Map<String, String> parseRequest(String request) {

        Map<String, String> map = new HashMap<>();

        request = request.replace("{", "")
                .replace("}", "");

        String[] pairs = request.split(",");

        for (String pair : pairs) {
            String[] keyValue = pair.split("=");
            if (keyValue.length == 2) {
                map.put(keyValue[0].trim(), keyValue[1].trim());
            }
        }

        return map;
    }

}