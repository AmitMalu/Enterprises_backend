package com.im.admin.entities;

import com.im.admin.dto.AbstractWalletAmountTable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@Entity
@Table(name = "partner_prepaid_amount")
public class PartnerPrepaidAmount extends AbstractWalletAmountTable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @OneToOne
    @JoinColumn(name = "partner_id", nullable = false, unique = true)
    private Partner partnerId;

    @Column(name = "total_cash")
    private BigDecimal totalCash = BigDecimal.ZERO;

    @Column(name = "used_cash")
    private BigDecimal usedCash = BigDecimal.ZERO;

    @Column(name = "cut_of_amount")
    private BigDecimal cutOfAmount = BigDecimal.ZERO;

    @Column(name = "available_balance")
    private BigDecimal availableBalance = BigDecimal.ZERO;

    @Column(name = "last_updated_date")
    private LocalDate lastUpdatedDate;

    @Column(name = "last_updated_time")
    private LocalTime lastUpdatedTime;

    @Column(name = "last_updated_amount")
    private BigDecimal lastUpdatedAmount = BigDecimal.ZERO;

}