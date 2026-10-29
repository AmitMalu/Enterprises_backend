package com.scs.admin.entities;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "bbps_provider_list")
public class BbpsProviderList {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "operator")
    private String operator;

    @Column(name = "sub_operator")
    private String subOperator;

    @Column(name = "vendor_id")
    private Integer vendorId;
}
