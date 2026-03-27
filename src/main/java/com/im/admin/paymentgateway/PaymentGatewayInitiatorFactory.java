package com.im.admin.paymentgateway;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class PaymentGatewayInitiatorFactory {

    private final Map<String, PaymentGatewayInitiator> initiatorMap;

    public PaymentGatewayInitiatorFactory(List<PaymentGatewayInitiator> initiators) {
        this.initiatorMap = initiators.stream()
                .collect(Collectors.toMap(
                        PaymentGatewayInitiator::getVendorName,
                        Function.identity()
                ));
    }

    public PaymentGatewayInitiator get(String vendorName) {
        PaymentGatewayInitiator initiator = initiatorMap.get(vendorName);

        if (initiator == null) {
            throw new RuntimeException("Vendor not supported: " + vendorName);
        }

        return initiator;
    }
}
