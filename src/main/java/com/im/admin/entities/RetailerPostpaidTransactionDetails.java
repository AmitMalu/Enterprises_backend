package com.im.admin.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.sql.Time;
import java.util.Date;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "retailer_postpaid_transaction_details")
public class RetailerPostpaidTransactionDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "transaction_id")
    private String transactionId;

    @Column(name = "date_of_transaction")
    private Date dateOfTransaction;

    @Column(name = "time_of_transaction")
    private Time timeOfTransaction;

    @Column(name = "service")
    private String service;

    @Column(name = "ch_bal_before_tran")
    private BigDecimal chBalBeforTran;

    @Column(name = "request_amount")
    private BigDecimal requestAmount;

    @Column(name = "comission")
    private BigDecimal commission;

    @Column(name = "service_charges")
    private BigDecimal serviceCharge;

    @Column(name = "amount")
    private BigDecimal amount;

    @Column(name = "action_onbalance_amount")
    private BigDecimal actionOnbalanceAmount;

    @Column(name = "ch_bal_after_tran")
    private BigDecimal chBalAfterTran;

    @Column(name = "tran_status")
    private String tranStatus;

    @Column(name = "ch_final_balance")
    private BigDecimal chFinalBalance;

    @Column(name = "REMARKS")
    private String remarks;

    @Column(name = "`card no`")
    private String cardNo;

    @Column(name = "payment_mode")
    private String paymentMode;

    @Column(name = "retailer_id")
    private String retailerId;


}
