package com.im.admin.entities;

import com.im.admin.dto.AbstractRetailerAmount;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.sql.Time;
import java.util.Date;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "admin_prepaid_amount")
public class AdminPrepaidAmount extends AbstractRetailerAmount {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @OneToOne
    @JoinColumn(name = "admin_id", nullable = false, unique = true)
    private Admin adminId;

    @Column(name = "total_cash", precision = 10, scale = 2)
    private BigDecimal totalCash = BigDecimal.ZERO;

    @Column(name = "used_cash", precision = 10, scale = 2)
    private BigDecimal usedCash = BigDecimal.ZERO;

    @Column(name = "cut_of_amount", precision = 10, scale = 2)
    private BigDecimal cutOfAmount = BigDecimal.ZERO;

    @Column(name = "available_balance", precision = 10, scale = 2)
    private BigDecimal availableBalance = BigDecimal.ZERO;

    @Column(name = "last_updated_date")
    private Date lastUpdatedDate = new Date(System.currentTimeMillis());

    @Column(name = "last_updated_time")
    private Time lastUpdatedTime = new Time(System.currentTimeMillis());

    @Column(name = "last_updated_amount", precision = 10, scale = 2)
    private BigDecimal lastUpdatedAmount = BigDecimal.ZERO;

}
