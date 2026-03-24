package com.im.admin.service.impl;

import com.im.admin.dto.BbpsTranactionRequestDto;
import com.im.admin.entities.BBPSPendingTransactions;
import com.im.admin.repositories.BBPSPendingTransactionsRepository;
import com.im.admin.response.BillRechargeCheckStatusResponseDto;
import com.im.admin.util.BBPSRelatedTransactions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Slf4j
@Component
public class RechargeStatusService {

    @Autowired
    private BBPSPendingTransactionsRepository bbpsPendingTransactionsRepository;

    @Autowired
    private BBPSRelatedTransactions bbpsRelatedTransactions;

    RestTemplate restTemplate = new RestTemplate();

    public void checkStatusForPendingTransactions() {

        List<BBPSPendingTransactions> pendingList = bbpsPendingTransactionsRepository.findByStatus("PENDING");
        log.info("Checking status for {} pending transactions", pendingList.size());

        for (BBPSPendingTransactions txn : pendingList) {
            try {
                MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
                params.add("api_token", txn.getToken());
                params.add("userId", txn.getUsername());
                params.add("secretKey", txn.getClientSecret());
                params.add("clientId", txn.getTransactionId());

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
                HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(params, headers);

                ResponseEntity<BillRechargeCheckStatusResponseDto> response = restTemplate.exchange(
                        txn.getVendorBaseUrl() + "/v3/check-status",
                        HttpMethod.POST,
                        entity,
                        BillRechargeCheckStatusResponseDto.class
                );

                BillRechargeCheckStatusResponseDto resp = response.getBody();
                if (resp == null) {
                    log.warn("Null response for transaction: {}", txn.getTransactionId());
                    continue;
                }

                // Build DTO from saved info
                BbpsTranactionRequestDto dto = new BbpsTranactionRequestDto(
                        txn.getSchemeRate(),
                        txn.getAmount().doubleValue(),
                        txn.getWalletType(),
                        txn.getRetailerId(),
                        txn.getGst(),
                        txn.getTds(),
                        resp.getMessage(),
                        txn.getTransactionId(),
                        txn.getOperatorName(),
                        txn.getSubProductName(),
                        0.0,
                        0.0,
                        "-",
                        "Not Yet"
                );

                if ("SUCCESS".equalsIgnoreCase(resp.getMessage()) || (resp.getStatus() != null && resp.getStatus() == 43)) {
                    log.info("Transaction {} SUCCESS in background check", txn.getTransactionId());
                    bbpsRelatedTransactions.updateTransactionStatus(txn.getRetailerId(), txn.getWalletType(), txn.getTransactionId());
                    bbpsRelatedTransactions.processCommission(dto);

                    txn.setStatus("SUCCESS");
                }
                else if ("FAILED".equalsIgnoreCase(resp.getMessage()) || (resp.getStatus() != null && resp.getStatus() == 55)) {
                    log.info("Transaction {} FAILED in background check", txn.getTransactionId());
                    bbpsRelatedTransactions.reverseTransactionsAndUpdateWallet(txn.getRetailerId(), txn.getWalletType(), txn.getTransactionId());
                    txn.setStatus("FAILED");
                }

                bbpsPendingTransactionsRepository.save(txn);

            } catch (Exception ex) {
                log.error("Error checking status for transaction {}: {}", txn.getTransactionId(), ex.getMessage());
            }
        }
    }
}
