package com.im.admin.entities.product;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;


@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
class Account {

    @NotNull(message = "Minimum postpaid balance is required")
    @Column(name = "minimum_postpaid_balance", nullable = false)
    private Double minimumPostpaidBalance;

    @NotNull(message = "Minimum prepaid balance is required")
    @Column(name = "minimum_prepaid_balance", nullable = false)
    private Double minimumPrepaidBalance;

    @NotNull(message = "Minimum Gullak balance is required")
    @Column(name = "minimum_gullak_balance", nullable = false)
    private Double minimumGullakBalance;

    @NotNull(message = "Minimum single transaction amount is required")
    @Column(name = "minimum_single_transaction_amount", nullable = false)
    private Double minimumSingleTransactionAmount;

    @NotNull(message = "Maximum single transaction amount is required")
    @Column(name = "maximum_single_transaction_amount", nullable = false)
    private Double maximumSingleTransactionAmount;

   // @NotNull(message = "Surcharge status is required")
    //@Column(name = "surcharge", nullable = false)
    //private Boolean surcharge;

    @NotNull(message = "CGST is required")
    @Column(name = "cgst", nullable = false)
    private Double cgst;

    @NotNull(message = "SGST is required")
    @Column(name = "sgst", nullable = false)
    private Double sgst;

    @NotNull(message = "IGST is required")
    @Column(name = "igst", nullable = false)
    private Double igst;

    @NotNull(message = "UGST is required")
    @Column(name = "ugst", nullable = false)
    private Double ugst;

    @NotNull(message = "CESS1 is required")
    @Column(name = "cess1", nullable = false)
    private Double cess1;

    @NotNull(message = "CESS2 is required")
    @Column(name = "cess2", nullable = false)
    private Double cess2;

    @NotNull(message = "TDS is required")
    @Column(name = "tds", nullable = false)
    private Double tds;
}

