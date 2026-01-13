package com.im.admin.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "aeps_mini_statement")
public class AepsMiniStatement {

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

    @Lob
    @Column(name = "request_payload", columnDefinition = "LONGTEXT")
    private String requestPayload;

    @Lob
    @Column(name = "response_payload", columnDefinition = "LONGTEXT")
    private String responsePayload;
}
