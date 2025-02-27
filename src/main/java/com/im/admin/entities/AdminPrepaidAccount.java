package com.im.admin.entities;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.sql.Time;
import java.util.Date;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "Admin_prepaid_account")
public class AdminPrepaidAccount {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "total_cash")
    private BigDecimal totalCash;

    @Column(name = "used_cash")
    private BigDecimal usedCash;

    @Column(name = "cut_of_amount")
    private BigDecimal cutOfAmount;

    @Column(name = "available_balance")
    private BigDecimal availableBalance;

    @Column(name = "last_updated_date")
    private Date lastUpdateDate;

    @Column(name = "last_updated_time")
    private Time lastUpdateTime;

    @Column(name = "last_updated_amount")
    private BigDecimal lastUpdatedAmount;

}
