package com.scs.admin.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.scs.admin.dto.BbpsTranactionRequestDto;
import com.scs.admin.entities.BBPSPendingTransactions;
import com.scs.admin.repositories.BBPSPendingTransactionsRepository;
import com.scs.admin.request.TransactionStatusRequest;
import com.scs.admin.service.BillAvenueService;
import com.scs.admin.util.BBPSRelatedTransactions;
import com.scs.admin.util.RequestIdUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class BillAvenueStatusService {

    @Autowired
    private BBPSPendingTransactionsRepository pendingRepo;

    @Autowired
    private BBPSRelatedTransactions bbpsRelatedTransactions;

    @Autowired
    private BillAvenueService billAvenueService;

    public void checkPendingTransactions() {

        List<BBPSPendingTransactions> pendingList =
                pendingRepo.findByStatus("PENDING");

        for (BBPSPendingTransactions txn : pendingList) {

            try {
                // ===============================
                // Build status request (CORRECT)
                // ===============================
                TransactionStatusRequest req = new TransactionStatusRequest();
                req.setTrackingType("TRANS_REF_ID");
//                req.setTrackingValue(txn.getVendorTxnRefId());   // ** add field when u do for production
                req.setRequestId(RequestIdUtil.generate());

                JsonNode response =
                        (JsonNode) billAvenueService.fetchTransactionStatus(req);

                if (response == null || !response.has("responseCode")) {
                    log.warn("Invalid response for txn {}", txn.getTransactionId());
                    continue;
                }

                String responseCode = response.get("responseCode").asText();
                String txnRespType = response.has("txnRespType")
                        ? response.get("txnRespType").asText()
                        : "";

                // ================= SUCCESS =================
                if ("000".equals(responseCode)) {

                    bbpsRelatedTransactions.updateTransactionStatus(
                            txn.getRetailerId(),
                            txn.getWalletType(),
                            txn.getTransactionId()
                    );

                    BbpsTranactionRequestDto dto =
                            buildDtoFromPending(txn);

                    bbpsRelatedTransactions.processCommission(dto);

                    txn.setStatus("SUCCESS");
                }

                // ================= STILL PENDING =================
                else if ("001".equals(responseCode)
                        || "FORWARD".equalsIgnoreCase(txnRespType)) {

                    // ❗ Do nothing, leave as PENDING
                    continue;
                }

                // ================= FAILURE =================
                else {

                    bbpsRelatedTransactions.reverseTransactionsAndUpdateWallet(
                            txn.getRetailerId(),
                            txn.getWalletType(),
                            txn.getTransactionId()
                    );

                    txn.setStatus("FAILED");
                }

                pendingRepo.save(txn);

            } catch (Exception e) {
                log.error("BillAvenue CRON error for txn {}",
                        txn.getTransactionId(), e);
            }
        }
    }

    // ===============================
    // Build DTO from pending txn
    // ===============================
    private BbpsTranactionRequestDto buildDtoFromPending(
            BBPSPendingTransactions txn) {

        return new BbpsTranactionRequestDto(
                txn.getSchemeRate(),
                txn.getAmount().doubleValue(),
                txn.getWalletType(),
                txn.getRetailerId(),
                txn.getGst(),
                txn.getTds(),
                "SUCCESS",
                txn.getTransactionId().replace("TXN", ""),
                txn.getOperatorName(),
                txn.getSubProductName(),
                0.0,
                0.0,
                "-",
                "Not Yet"
        );
    }
}
