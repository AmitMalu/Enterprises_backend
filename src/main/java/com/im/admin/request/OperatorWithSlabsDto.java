package com.im.admin.request;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OperatorWithSlabsDto {

    private String operatorCode;
    private String operatorName;
    private List<SlabRateDto> slabs;
}
