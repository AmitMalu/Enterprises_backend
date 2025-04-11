package com.im.admin.dto;

import com.im.admin.entities.enums.RateType;
import com.im.admin.entities.enums.SlabType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SchemeRateSlabDTO {
    private RateType rateType;

    private Double slabStart;       // Used if SlabType is VARIABLE
    private Double slabEnd;         // Used if SlabType is VARIABLE

    private Double gross;
    private Double im;
    private Double enterprise;
    private Double whitelabel;
    private Double superDistributor;
    private Double partner;
    private Double distributor;
    private Double retailer;
}
