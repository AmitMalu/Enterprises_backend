package com.scs.admin.entities;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "state")
public class State {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "zone")
    private String zone;

    @Column(name = "state_name")
    private String stateName;

    @Column(name = "state_code")
    private String stateCode;

    @Column(name = "region_type")
    private String regionType;

    @ManyToOne
    @JoinColumn(name = "country_id")
    private Country country;

    @Column(name = "blocked")
    private Boolean blocked = false;

    @Column(name = "created_on")
    private LocalDateTime createdOn = LocalDateTime.now();

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "edited_on")
    private LocalDateTime editedOn;

    @Column(name = "edited_by")
    private String editedBy;
}
