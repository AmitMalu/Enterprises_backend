package com.im.admin.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "dmt_transaction")
@Getter
@Setter
public class DmtTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String orderId;            // internal transaction ID
    private String ackno;              // vendor ack number
    private String utr;                // unique transaction ref (bank)
    private Integer responseCode;      // vendor response code
    private Integer txnStatus;         // txn_status from vendor
    private String vendorStatus;       // SUCCESS / PENDING / FAILED (mapped)
    private String beneName;
    private String remarks;
    private String message;
    private String remitter;
    private String accountNumber;
    private String txnAmount;
    private Double balance;
    private String referenceId;

    private LocalDateTime createdAt = LocalDateTime.now();
}
