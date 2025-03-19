package com.im.admin.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.sql.Time;
import java.util.Date;

@Getter
@Setter
@Entity
@Table(name = "white_label_prepaid_amount")
public class WhiteLabelPrepaidAmount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @OneToOne
    @JoinColumn(name = "whitelabel_id", nullable = false, unique = true)
    private WhiteLabels whiteLabelId;

    @Column(name = "total_cash", precision = 10, scale = 2)
    private BigDecimal totalCash = BigDecimal.ZERO;

    @Column(name = "used_cash", precision = 10, scale = 2)
    private BigDecimal usedCash = BigDecimal.ZERO;

    @Column(name = "cut_of_amount", precision = 10, scale = 2)
    private BigDecimal cutOfAmount = BigDecimal.ZERO;

    @Column(name = "available_balance", precision = 10, scale = 2)
    private BigDecimal availableBalance = BigDecimal.ZERO;

    @Column(name = "last_updated_date")
    private Date lastUpdateDate = new Date();

    @Column(name = "last_updated_time")
    private Time lastUpdateTime = new Time(System.currentTimeMillis());

    @Column(name = "last_updated_amount", precision = 10, scale = 2)
    private BigDecimal lastUpdatedAmount = BigDecimal.ZERO;

}