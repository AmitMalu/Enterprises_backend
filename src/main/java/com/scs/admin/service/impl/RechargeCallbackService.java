package com.scs.admin.service.impl;

import com.scs.admin.entities.RechargeCallback;
import com.scs.admin.entities.RechargeTransaction;
import com.scs.admin.repositories.RechargeCallbackRepository;
import com.scs.admin.repositories.RechargeTransactionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
public class RechargeCallbackService {

    @Autowired
    private RechargeCallbackRepository rechargeCallbackRepository;

    @Autowired
    private RechargeTransactionRepository rechargeTransactionRepository;

    public void processCallback(String id,
                                String transactionRef,
                                String status,
                                String message,
                                String vendorTxnId,
                                String operatorTxnId,
                                String preBalance,
                                String postBalance,
                                String discountPercent,
                                String discountRs) {



        // 1Log full callback request
        log.info("Recharge Callback Received -> ID: {}, TNO: {}, ST: {}, MSG: {}, VendorTxnId: {}, OperatorTxnId: {}, PRB: {}, POB: {}, DP: {}, DR: {}",
                id, transactionRef, status, message, vendorTxnId, operatorTxnId,
                preBalance, postBalance, discountPercent, discountRs);

        //  Save callback in recharge_callback table
        RechargeCallback callback = new RechargeCallback();
        callback.setApiCode(id);
        callback.setTransactionRef(transactionRef);
        callback.setStatus(status);
        callback.setStatusMessage(message);
        callback.setVendorTxnId(vendorTxnId);
        callback.setOperatorTxnId(operatorTxnId);
        callback.setPreBalance(preBalance);
        callback.setPostBalance(postBalance);
        callback.setDiscountPercent(discountPercent);
        callback.setDiscountRs(discountRs);
        callback.setReceivedAt(LocalDateTime.now().now());

        rechargeCallbackRepository.save(callback);

        log.info("Recharge callback saved in DB for TxnRef: {}", transactionRef);

        // 3Update recharge transaction
        RechargeTransaction txn =
                rechargeTransactionRepository
                        .findByTransactionRef(transactionRef)
                        .orElseThrow(() -> {
                            log.error("Recharge transaction not found for TxnRef: {}", transactionRef);
                            return new RuntimeException("Transaction not found");
                        });

        if ("1".equals(status)) {
            txn.setStatus("SUCCESS");
        }
        else if ("2".equals(status) || "3".equals(status)) {
            txn.setStatus("FAILED");
        }
        else if ("5".equals(status)) {
            txn.setStatus("REFUNDED");
        }
        else {
            txn.setStatus("PENDING");
        }

//        txn.setVendorTransactionId(vendorTxnId);
//        txn.setOperatorTransactionId(operatorTxnId);
        txn.setVendorResponse(message);

        rechargeTransactionRepository.save(txn);

        log.info("RechargeTransaction updated successfully for TxnRef: {} with Status: {}",
                transactionRef, txn.getStatus());
    }

}

