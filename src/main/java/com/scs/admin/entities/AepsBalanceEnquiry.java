package com.scs.admin.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "aeps_balance_enquiry")
public class AepsBalanceEnquiry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")   // optional
    private Long id;

    @Column(name = "retailer_id")
    private String retailerId;

    @Column(name = "vendor_id")
    private String vendorId;

    @Column(name = "fingpay_transaction_id", length = 100)
    private String fingpayTransactionId;

    @Column(name = "bank_rrn", length = 100)
    private String bankRrn;

    @Column(name = "merchant_txn_id", length = 100)
    private String merchantTxnId;

    @Column(name = "terminal_id", length = 50)
    private String terminalId;

    @Column(name = "transaction_status", length = 50)
    private String transactionStatus;

    @Column(name = "balance_amount")
    private Double balanceAmount;

    @Column(name = "transaction_id")
    private String transactionId;

    @Column(name = "aadhaar_number")
    private String aadhaarNumber;

    @Column(name = "mobile_number")
    private String mobileNumber;

    @Column(name = "bank_name")
    private String bankName;

    @Column(name = "transaction_amount")
    private Double transactionAmount;

    @Column(name = "status_message")
    private String statusMessage;

    @Lob
    @Column(name = "request_payload", columnDefinition = "LONGTEXT")
    private String requestPayload;

    @Lob
    @Column(name = "response_payload", columnDefinition = "LONGTEXT")
    private String responsePayload;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

}
