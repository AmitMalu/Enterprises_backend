package com.scs.admin.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "aeps_cash_withdrawal_log")
public class AepsCashWithdrawalLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "retailer_id", length = 50)
    private String retailerId;

    @Column(name = "vendor_id")
    private String vendorId;

    @Column(name = "fingpay_transaction_id", length = 100)
    private String fingpayTransactionId;

    @Column(name = "bank_rrn", length = 100)
    private String bankRrn;

    @Column(name = "merchant_txn_id", length = 100)
    private String merchantTxnId;

    @Column(name = "transaction_status", length = 50)
    private String transactionStatus;

    @Column(name = "transaction_amount")
    private Double transactionAmount;

    @Column(name = "balance_amount")
    private Double balanceAmount;

    @Column(name = "request_payload", columnDefinition = "LONGTEXT")
    private String requestPayload;

    @Column(name = "response_payload", columnDefinition = "LONGTEXT")
    private String responsePayload;

    @Column(name = "transaction_id")
    private String transactionId;

    @Column(name = "aadhaar_number")
    private String aadhaarNumber;

    @Column(name = "mobile_number")
    private String mobileNumber;

    @Column(name = "bank_name")
    private String bankName;

    @Column(name = "status_message")
    private String statusMessage;

    @Column(name = "terminal_id", length = 50)
    private String terminalId;


    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }


}
