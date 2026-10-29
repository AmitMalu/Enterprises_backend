package com.scs.admin.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class SchemeAllocationRequestDto {

    private String hierarchyId;  // Can be Retailer, Distributor, Partner, etc.
    private List<SchemeAllocationDto> codes;
}
