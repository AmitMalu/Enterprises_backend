package com.im.admin.entities;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;


@Getter
@Setter
@Entity
@Table(name = "enterprises_postpaid_transaction_details")
public class EnterprisesPostpaidTransactionDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "enterprises_id")
    private String enterprisesId;

    @Column(name = "date_of_transaction")
    private LocalDate dateOfTransaction;

    @Column(name = "time_of_transaction", columnDefinition = "TIME(3)")
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

    @Column(name = "service")  //(Charge/GST/Commission/TDS)
    private String service;

    @Column(name = "updated_date_of_transaction")
    private LocalDate updatedDateOfTransaction;

    @Column(name = "updated_time_of_transaction", columnDefinition = "TIME(3)")
    private LocalDate updatedTimeOfTransaction;

    @Column(name = "mobile_no")
    private String mobileNo;

    @Column(name = "scheme_name")
    private String schemeName;

    @Column(name = "sub_product_name")
    private String subProductName;

    @Column(name = "operator_name")
    private String operatorName;

    @Column(name = "sub_operator_name")
    private String subOperatorName;

    @Column(name = "REMARKS")
    private String remarks;

    @Column(name = "transaction_id")
    private String transactionId;

    @Column(name = "narration")
    private String narration;

}
