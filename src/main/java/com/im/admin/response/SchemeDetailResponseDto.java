package com.im.admin.response;

import com.im.admin.request.OperatorWithSlabsDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SchemeDetailResponseDto {

    private String schemeCode;
    private String schemeName;
    private Boolean isActive;
    private double tdsRate;
    private String productName;
    private List<OperatorWithSlabsDto> operators;
}
