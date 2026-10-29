package com.scs.admin.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "billavenue_transaction")
public class BillAvenueTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime createdDateTime;

    private String txnStatus;        // SUCCESS | FAILED | PENDING
    private String billerName;
    private Long  txnAmount;
    private String customerName;
    private String consumerNumber;
    private String category;
    private String txnId;
    private String referenceNumber;
}

