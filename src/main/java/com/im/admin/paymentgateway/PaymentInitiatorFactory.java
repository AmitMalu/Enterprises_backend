package com.im.admin.paymentgateway;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class PaymentInitiatorFactory {

    private final Map<String, PaymentInitiator> initiatorMap;

    public PaymentInitiatorFactory(List<PaymentInitiator> initiators) {
        this.initiatorMap = initiators.stream()
                .collect(Collectors.toMap(
                        PaymentInitiator::getVendorName,
                        Function.identity()
                ));
    }

    public PaymentInitiator get(String vendorName) {
        PaymentInitiator initiator = initiatorMap.get(vendorName);

        if (initiator == null) {
            throw new RuntimeException("Vendor not supported: " + vendorName);
        }

        return initiator;
    }
}
