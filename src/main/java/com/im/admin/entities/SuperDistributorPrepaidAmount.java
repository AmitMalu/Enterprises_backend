package com.im.admin.entities;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;


import java.math.BigDecimal;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Date;

@Getter
@Setter
@Entity
@Table(name = "super_distributor_prepaid_amount")
public class SuperDistributorPrepaidAmount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @OneToOne
    @JoinColumn(name = "superDistributor_id", nullable = false, unique = true)
    private SuperDistributor superDistributorId;

    @Column(name = "total_cash")
    private BigDecimal totalCash = BigDecimal.ZERO;

    @Column(name = "used_cash")
    private BigDecimal usedCash = BigDecimal.ZERO;

    @Column(name = "cut_of_amount")
    private BigDecimal cutOfAmount = BigDecimal.ZERO;

    @Column(name = "available_balance")
    private BigDecimal availableBalance = BigDecimal.ZERO;

    @Column(name = "last_updated_date")
    private Date lastUpdateDate = new Date(System.currentTimeMillis());

    @Column(name = "last_updated_time")
    private Time lastUpdateTime = new Time(System.currentTimeMillis());

    @Column(name = "last_updated_amount")
    private BigDecimal lastUpdatedAmount = BigDecimal.ZERO;

}