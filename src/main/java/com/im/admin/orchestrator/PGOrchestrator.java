package com.im.admin.orchestrator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.im.admin.entities.PgTransactionMapping;
import com.im.admin.entities.Scheme;
import com.im.admin.entities.VendorRouting;
import com.im.admin.paymentgateway.PGCreateOrderCommand;
import com.im.admin.paymentgateway.PaymentGatewayInitiator;
import com.im.admin.paymentgateway.PaymentGatewayInitiatorFactory;
import com.im.admin.repositories.PgTransactionMappingRepository;
import com.im.admin.repositories.SchemeRepository;
import com.im.admin.repositories.VendorRoutingRepository;
import com.im.admin.response.PgStatusResponse;
import com.im.admin.response.PgUnifiedOrderResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PGOrchestrator {

    private final VendorRoutingRepository vendorRoutingRepository;
    private final PgTransactionMappingRepository pgTransactionMappingRepository;
    private final PaymentGatewayInitiatorFactory factory;
    private final SchemeRepository schemeRepository;

    // CREATE ORDER
    public PgUnifiedOrderResponse initiate(PGCreateOrderCommand command, Double latitude, Double longitude) throws JsonProcessingException {

        log.info("PG Initiate request received. subProductId={}, amount={}, initiatedBy={}",
                command.getSubProductId(),
                command.getAmount(),
                command.getInitiatedBy());

        try {

            Scheme scheme = schemeRepository.findBySchemeCode(command.getSchemeCode())
                    .orElseThrow(() -> new RuntimeException("Scheme is not activated. Please contact admin"));

            if (!Boolean.TRUE.equals(scheme.getIsActive())) {
                log.warn("Scheme is inactive for schemeCode={}", command.getSchemeCode());
                throw new RuntimeException("Scheme is inactive. Please contact admin");
            }

            log.debug("Scheme is activate ={}",
                    command.getSchemeCode());

            log.debug("Fetching vendor routing for subProductId={}",
                    command.getSubProductId());

            VendorRouting routing =
                    vendorRoutingRepository
                            .findBySubProductId(command.getSubProductId());

            if (routing == null) {
                log.error("No routing found for subProductId={}",
                        command.getSubProductId());
                throw new RuntimeException("Vendor routing not found");
            }

            String vendorName =
                    routing.getRoutingPriority1()
                            .getVendorName();

            log.info("Vendor selected for subProductId={} is vendorName={}",
                    command.getSubProductId(),
                    vendorName);

            PaymentGatewayInitiator initiator =
                    factory.get(vendorName);

            log.debug("PaymentInitiator resolved for vendorName={}", vendorName);

            PgUnifiedOrderResponse response =
                    initiator.initiate(command, latitude, longitude);

            log.info("PG initiate completed successfully for vendorName={}, subProductId={}",
                    vendorName,
                    command.getSubProductId());

            return response;

        } catch (Exception e) {
            log.error("Error during PG initiate for subProductId={}",
                    command.getSubProductId(),
                    e);
            throw e;
        }
    }

    @Transactional
    public void checkStatusForPendingTransactions() {

        List<PgTransactionMapping> pendingList = pgTransactionMappingRepository.findByStatus("PENDING");
        log.info("[ORCH_SCHEDULER] Found {} pending transactions", pendingList.size());

        for (PgTransactionMapping txn : pendingList) {
            String orderId = txn.getOrderId();
            log.info("[1] Checking status for pending ORDER_ID={}", orderId);

            try {

                PaymentGatewayInitiator initiator =
                        factory.get(txn.getVendorName());

                PgStatusResponse vendorResponse = initiator.checkStatus(orderId);

            } catch (Exception e) {
                log.error("[ORCH_SCHEDULER] Error while checking status for ORDER_ID={} | {}",
                        orderId, e.getMessage(), e);
            }
        }
    }
}