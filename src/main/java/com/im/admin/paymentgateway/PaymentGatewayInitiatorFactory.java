package com.im.admin.paymentgateway;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@Slf4j
public class PaymentGatewayInitiatorFactory {

    private final Map<String, PaymentGatewayInitiator> initiatorMap;

//    public PaymentGatewayInitiatorFactory(List<PaymentGatewayInitiator> initiators) {
//        this.initiatorMap = initiators.stream()
//                .collect(Collectors.toMap(
//                        PaymentGatewayInitiator::getVendorName,
//                        Function.identity()
//                ));
//    }

    public PaymentGatewayInitiatorFactory(List<PaymentGatewayInitiator> initiators) {
        log.info("Initializing PaymentGatewayInitiatorFactory with {} initiators",
                initiators != null ? initiators.size() : 0);

        if (initiators == null || initiators.isEmpty()) {
            log.warn("No payment gateway initiators provided! Factory will be empty.");
            this.initiatorMap = Map.of();
            return;
        }

        // Log all available vendors
        List<String> vendorNames = initiators.stream()
                .map(PaymentGatewayInitiator::getVendorName)
                .collect(Collectors.toList());
        log.debug("Available payment gateway vendors: {}", vendorNames);

        this.initiatorMap = initiators.stream()
                .collect(Collectors.toMap(
                        PaymentGatewayInitiator::getVendorName,
                        Function.identity()
                ));

        log.info("PaymentGatewayInitiatorFactory initialized successfully with {} vendors: {}",
                initiatorMap.size(), initiatorMap.keySet());
    }

//    public PaymentGatewayInitiator get(String vendorName) {
//        PaymentGatewayInitiator initiator = initiatorMap.get(vendorName);
//
//        if (initiator == null) {
//            throw new RuntimeException("Vendor not supported: " + vendorName);
//        }
//
//        return initiator;
//    }

    public PaymentGatewayInitiator get(String vendorName) {
        log.debug("Attempting to retrieve payment gateway initiator for vendor: {}", vendorName);

        if (vendorName == null || vendorName.trim().isEmpty()) {
            log.error("Vendor name is null or empty");
            throw new IllegalArgumentException("Vendor name cannot be null or empty");
        }

        PaymentGatewayInitiator initiator = initiatorMap.get(vendorName);

        if (initiator == null) {
            log.error("Vendor not supported: {}. Available vendors: {}",
                    vendorName, initiatorMap.keySet());
            throw new RuntimeException("Vendor not supported: " + vendorName +
                    ". Available vendors: " + initiatorMap.keySet());
        }

        log.debug("Successfully retrieved payment gateway initiator for vendor: {}", vendorName);
        return initiator;
    }
}
