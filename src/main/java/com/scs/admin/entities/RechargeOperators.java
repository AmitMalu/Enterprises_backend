package com.scs.admin.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;


@Entity
@Table(name = "recharge_operators")
@Getter
@Setter
public class RechargeOperators extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;          // Airtel
    private String code;          // A
    private String type;          // MOBILE / DTH / POSTPAID
    private Boolean active;
    @Column(name = "spkey")
    private String spKey;
}
