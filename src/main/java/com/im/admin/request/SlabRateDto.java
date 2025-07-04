package com.im.admin.request;

import com.im.admin.entities.enums.RateType;
import com.im.admin.entities.enums.SlabType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SlabRateDto {

    private Long id;
    private RateType rateType;
    private SlabType slabType;
    private Double fixedSlabValue;
    private Double slabStart;
    private Double slabEnd;
    private Double gross;
    private Double im;
    private Double enterprise;
    private Double whitelabel;
    private Double superDistributor;
    private Double partner;
    private Double distributor;
    private Double retailer;
    private String createdBY;
    private String editedBy;

}
