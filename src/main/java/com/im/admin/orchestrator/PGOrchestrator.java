package com.im.admin.orchestrator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.im.admin.entities.PgTransactionMapping;
import com.im.admin.entities.VendorRouting;
import com.im.admin.paymentgateway.PGCreateOrderCommand;
import com.im.admin.paymentgateway.PaymentInitiator;
import com.im.admin.paymentgateway.PaymentInitiatorFactory;
import com.im.admin.repositories.PgTransactionMappingRepository;
import com.im.admin.repositories.VendorRoutingRepository;
import com.im.admin.response.PgUnifiedOrderResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class PGOrchestrator {

    private final VendorRoutingRepository vendorRoutingRepository;
    private final PgTransactionMappingRepository pgTransactionMappingRepository;
    private final PaymentInitiatorFactory factory;

    // CREATE ORDER
    public PgUnifiedOrderResponse initiate(PGCreateOrderCommand command) throws JsonProcessingException {

        log.info("PG Initiate request received. subProductId={}, amount={}, initiatedBy={}",
                command.getSubProductId(),
                command.getAmount(),
                command.getInitiatedBy());

        try {

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

            PaymentInitiator initiator =
                    factory.get(vendorName);

            log.debug("PaymentInitiator resolved for vendorName={}", vendorName);

            PgUnifiedOrderResponse response =
                    initiator.initiate(command);

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

    // STATUS
    public ResponseEntity<?> checkStatus(String orderId) {

        PgTransactionMapping txn =
                pgTransactionMappingRepository
                        .findByOrderId(orderId)
                        .orElseThrow(() -> new RuntimeException("Order not found"));

        PaymentInitiator initiator =
                factory.get(txn.getVendorName());

        return initiator.checkStatus(orderId);
    }

}