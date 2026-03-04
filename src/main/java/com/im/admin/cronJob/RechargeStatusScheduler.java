package com.im.admin.cronJob;

import com.im.admin.entities.RechargeTransaction;
import com.im.admin.entities.VendorCredentials;
import com.im.admin.repositories.RechargeTransactionRepository;
import com.im.admin.repositories.VendorCredentialsRepository;
import com.im.admin.service.RechargeVendorService;
import com.im.admin.util.RechargeVendorFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RechargeStatusScheduler {

    @Autowired
    private RechargeTransactionRepository transactionRepository;

    @Autowired
    private VendorCredentialsRepository credentialsRepository;

    @Autowired
    private RechargeVendorFactory vendorFactory;

    // Runs every 30 seconds
    @Scheduled(fixedDelay = 30000)
    public void checkPendingTransactions() {

        List<RechargeTransaction> pendingList =
                transactionRepository.findByStatus("PENDING");

        for (RechargeTransaction txn : pendingList) {

            try {

                VendorCredentials credentials =
                        credentialsRepository
                                .findByVendorIdAndProductId(
                                        txn.getVendor().getId(),
                                        txn.getVendor().getProduct().getId())
                                .orElse(null);

                if (credentials == null) continue;

                RechargeVendorService vendorService =
                        vendorFactory.getVendor(txn.getVendor().getVendorName());

                // Call status API
                String vendorResponse =
                        vendorService.checkStatusByTxnRef(
                                txn.getTransactionRef(),
                                credentials);

                // Parse and update status
                updateTransactionStatus(txn, vendorResponse);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void updateTransactionStatus(RechargeTransaction txn,
                                         String vendorResponse) {

        String[] parts = vendorResponse.split("=");

        int statusCode = Integer.parseInt(parts[0]);

        if (statusCode == 1) {
            txn.setStatus("SUCCESS");
        } else if (statusCode == 2 || statusCode == 3) {
            txn.setStatus("FAILED");
        } else if (statusCode == 4 || statusCode == 6) {
            txn.setStatus("HOLD");
        } else if (statusCode == 5) {
            txn.setStatus("REFUNDED");
        }

        txn.setVendorResponse(vendorResponse);

        transactionRepository.save(txn);
    }
}