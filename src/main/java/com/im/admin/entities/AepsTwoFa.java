package com.im.admin.entities;

import com.im.admin.entities.vendor.Vendors;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "aeps_twofa")
public class AepsTwoFa {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "tef_pk_id")
    private String tefPkId;

    @Column(name = "stan", length = 50)
    private String stan;

    @Column(name = "bank_rrn", length = 50)
    private String bankRrn;

    @Column(name = "fp_rrn", length = 50)
    private String fpRrn;

    @Column(name = "fingpay_transaction_id", length = 100)
    private String fingpayTransactionId;

    @Column(name = "merchant_tran_id", length = 100)
    private String merchantTranId;

    @Column(name = "response_code", length = 10)
    private String responseCode;

    @Column(name = "response_message", length = 255)
    private String responseMessage;

    @Column(name = "mobile_number", length = 15)
    private String mobileNumber;

    @Column(name = "transaction_timestamp", length = 50)
    private String transactionTimestamp;

    @Column(name = "retailer_id")
    private String retailerId;

    @Column(name = "created_at")
    private Long createdAt = System.currentTimeMillis();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id", nullable = false)
    private Vendors vendor;
}


