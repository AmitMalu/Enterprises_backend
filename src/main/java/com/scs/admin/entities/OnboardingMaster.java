package com.scs.admin.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "onboarding_master")
@Entity
public class OnboardingMaster {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "prepaid", nullable = false)
    private Double prepaid = 0.00;

    @Column(name = "postpaid", nullable = false)
    private Double postpaid = 0.00;

    @Column(name = "mobile_No")
    private String mobileNo;

}
