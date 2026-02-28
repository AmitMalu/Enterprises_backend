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
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PGOrchestrator {

    private final VendorRoutingRepository vendorRoutingRepository;
    private final PgTransactionMappingRepository pgTransactionMappingRepository;
    private final PaymentInitiatorFactory factory;

    // CREATE ORDER
    public PgUnifiedOrderResponse initiate(PGCreateOrderCommand command) throws JsonProcessingException {

        VendorRouting routing =
                vendorRoutingRepository
                        .findBySubProductId(command.getSubProductId());

        String vendorName =
                routing.getRoutingPriority1()
                        .getVendorName();

        PaymentInitiator initiator =
                factory.get(vendorName);

        return initiator.initiate(command);
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