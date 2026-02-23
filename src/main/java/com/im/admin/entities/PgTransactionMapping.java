package com.im.admin.entities;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "pg_transaction_mapping")
public class PgTransactionMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String orderId;
    private String initiatedBy;  // retailer_id
    private Double amount;
    private String status;       // INITIATED, SUCCESS, FAILED
    private String vendorName;
    private LocalDateTime createdAt = LocalDateTime.now();
    private String SubProductId;
    private String cardBinNo;
    private String TxnId;

}

