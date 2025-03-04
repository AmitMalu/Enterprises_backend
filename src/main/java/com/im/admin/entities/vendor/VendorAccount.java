package com.im.admin.entities.vendor;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VendorAccount {

    @Column(name = "transaction_rate_type")
    private String transactionRateType;

    @Column(name = "transaction_rate")
    private Double transactionRate;

    @Column(name = "acc_verification_charges")
    private Double accVerificationCharges;

    @Column(name = "api_charges")
    private Double apiCharges;

    @Column(name = "bank_charges")
    private Double bankCharges;

    @Column(name = "gst")
    private Double gst;
}


