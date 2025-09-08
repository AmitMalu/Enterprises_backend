package com.im.admin.entities;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "ledger_report_view")
@Getter
@Setter
public class LedgerReport {

    @Id
    @Column(name = "id")
    private String id; // unique id built in the view (source_prefix + transaction_id)

    @Column(name = "transaction_id")
    private String transactionId;

    @Column(name = "hierarchy_id")
    private String hierarchyId;

    @Column(name = "date_of_transaction")
    private LocalDate dateOfTransaction;

    @Column(name = "time_of_transaction")
    private LocalTime timeOfTransaction;

    @Column(name = "bal_before_tran")
    private BigDecimal balBeforeTran;

    @Column(name = "amount")
    private BigDecimal amount;

    @Column(name = "bal_after_tran")
    private BigDecimal balAfterTran;

    @Column(name = "final_balance")
    private BigDecimal finalBalance;

    @Column(name = "tran_status")
    private String tranStatus;

    @Column(name = "action_on_balance_amount")
    private String actionOnBalanceAmount;

    @Column(name = "service")
    private String service;

    @Column(name = "mobile_no")
    private String mobileNo;

    @Column(name = "sub_product_name")
    private String subProductName;

    @Column(name = "remarks")
    private String remarks;

    @Column(name = "narration")
    private String narration;

    @Column(name = "source_table")
    private String sourceTable;
}
