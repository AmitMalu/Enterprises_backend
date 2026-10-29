package com.scs.admin.entities;

import com.scs.admin.dto.AbstractWalletAmountTable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Getter
@Setter
@Entity
@Table(name = "white_label_postpaid_amount")
public class WhiteLabelPostpaidAmount extends AbstractWalletAmountTable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @OneToOne
    @JoinColumn(name = "white_label_id", nullable = false)
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
    private LocalDate lastUpdatedDate;

    @Column(name = "last_updated_time")
    private LocalTime lastUpdatedTime;

    @Column(name = "last_updated_amount", precision = 10, scale = 2)
    private BigDecimal lastUpdatedAmount = BigDecimal.ZERO;

    @Column(name = "remark", length = 500)
    private String remark;

}