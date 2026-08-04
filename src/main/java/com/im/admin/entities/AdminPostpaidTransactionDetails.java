package com.im.admin.entities;


import com.im.admin.dto.CommonSearchTransactionDetails;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@Entity
@Table(name = "admin_postpaid_transaction_details")
public class AdminPostpaidTransactionDetails implements CommonSearchTransactionDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "hierarchy_id")
    private String hierarchyId;

    @Column(name = "admin_id")
    private String adminId;

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

    @Column(name = "remarks")
    private String remarks;

    @Column(name = "transaction_id")
    private String transactionId;

    @Column(name = "narration")
    private String narration;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Override
    public String getVendorName() {
        return "";
    }

    @Override
    public String getVendorTransactionId() {
        return "";
    }
}
