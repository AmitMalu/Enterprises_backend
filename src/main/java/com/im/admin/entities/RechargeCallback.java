package com.im.admin.entities;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "recharge_callback")
public class RechargeCallback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "api_code")
    private String apiCode;

    @Column(name = "transaction_ref")
    private String transactionRef;

    @Column(name = "status")
    private String status;

    @Column(name = "status_message")
    private String statusMessage;

    @Column(name = "vendor_txn_id")
    private String vendorTxnId;

    @Column(name = "operator_txn_id")
    private String operatorTxnId;

    @Column(name = "pre_balance")
    private String preBalance;

    @Column(name = "post_balance")
    private String postBalance;

    @Column(name = "discount_percent")
    private String discountPercent;

    @Column(name = "discount_rs")
    private String discountRs;

    @Column(name = "received_at")
    private LocalDateTime receivedAt;
}
