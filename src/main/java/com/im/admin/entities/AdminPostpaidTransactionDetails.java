package com.im.admin.entities;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Date;

@Getter
@Setter
@Entity
@Table(name = "admin_postpaid_transaction_details")
public class AdminPostpaidTransactionDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "im_crn_no")
    private Long imCrnNo;

    @Column(name = "transaction_id")
    private String transactionId;

    @Column(name = "cust_bank_transaction_id")
    private String custBankTransactionId;

    @Column(name = "vendor_transaction_id")
    private String vendorTransactionId;

    @Column(name = "mobile_no")
    private String mobileNo;

    @Column(name = "date_of_transaction")
    private Date dateOfTransaction;

    @Column(name = "time_of_transaction")
    private Time timeOfTransaction;

    @Column(name = "service")
    private String service;

    @Column(name = "bal_before_tran")
    private BigDecimal balBeforeTran;

    @Column(name = "request_amount")
    private BigDecimal requestAmount;

    @Column(name = "commission")
    private BigDecimal commission;

    @Column(name = "service_charges")
    private BigDecimal serviceCharge;

    @Column(name = "amount")
    private BigDecimal amount;

    @Column(name = "action_on_balance_amount")
    private String actionOnBalanceAmount;

    @Column(name = "bal_after_tran")
    private BigDecimal balAfterTran;

    @Column(name = "tran_status")
    private String tranStatus;

    @Column(name = "final_balance")
    private BigDecimal finalBalance;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "REMARKS")
    private String remarks;

    @Column(name = "transmissiondttm")
    private Timestamp transmissiondttm;

    @Column(name = "payment_mode")
    private String paymentMode;

    @Column(name = "channel")
    private String channel;

    @Column(name = "terminal_id")
    private String terminalId;

    @Column(name = "terminal_accept_id")
    private String terminalAcceptId;

    @Column(name = "trace_number")
    private String traceNumber;

    @Column(name = "mcc")
    private Long mcc;

    @Column(name = "mcc_category")
    private String mccCategory;

    @Column(name = "merchant_name")
    private String merchantName;

    @Column(name = "kyc")
    private String kyc;

    @Column(name = "admin_id")
    private String adminId;


}
