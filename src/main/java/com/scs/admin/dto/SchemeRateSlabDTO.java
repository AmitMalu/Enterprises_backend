package com.scs.admin.dto;

import com.scs.admin.entities.enums.RateType;
import com.scs.admin.entities.enums.SlabType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SchemeRateSlabDTO {

    private Long id;
    private RateType rateType;
    private SlabType slabType;
    private Double slabStart;       // Used if SlabType is VARIABLE
    private Double slabEnd;         // Used if SlabType is VARIABLE

    private Double gross;
    private Double scs;
    private Double enterprise;
    private Double whitelabel;
    private Double superDistributor;
    private Double partner;
    private Double distributor;
    private Double retailer;
    private String createdBy;
    private String editedBy;
}
