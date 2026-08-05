package com.im.admin.entities;

import com.im.admin.dto.CommonSearchTransactionDetails;
import com.im.admin.util.AbstractRetailerTransactionDetails;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "retailer_postpaid_transaction_details")
public class RetailerPostpaidTransactionDetails implements AbstractRetailerTransactionDetails, CommonSearchTransactionDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "retailer_id")
    private String retailerId;

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
    private LocalTime updatedTimeOfTransaction;

    @Column(name = "mobile_no")
    private String mobileNo;

    @Column(name = "outlet_name")
    private String outletName;

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

    @Column(name = "transaction_type")
    private String transactionType;

    @Column(name = "vendor_name")
    private String vendorName;

    @Column(name = "vendor_transaction_id")
    private String vendorTransactionId;

    @Column(name = "bank_ref_id")
    private String bankRefId;

    @Column(name = "narration")
    private String narration;

    @Column(name = "failure_remarks")
    private String failureRemarks;

    @Column(name = "client_ip")
    private String clientIp;

    @Column(name = "medium")
    private String medium;        //(mobile/portal)

    @Column(name = "scheme_hierarchy_name")
    private String schemeHierarchyName;

    @Column(name = "card_holder_name")
    private String cardHolderName;

    @Column(name = "card_holder_mobile_number")
    private String cardHolderMobileNumber;

    @Column(name = "card_holder_bin_no")
    private String cardHolderBinNo;

    @Column(name = "card_type")
    private String card_type;

    @Column(name = "request_domain_name")
    private String requestDomainName;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Override
    public String getOperatorCode() {
        return "";
    }

    @Override
    public String getProductCode() {
        return "";
    }

    @Override
    public String getSubProductCode() {
        return "";
    }

    @Override
    public String getHierarchyId() {
        return retailerId;
    }
}
