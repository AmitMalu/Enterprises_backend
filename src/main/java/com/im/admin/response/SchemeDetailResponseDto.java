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
    private List<OperatorWithSlabsDto> operators;
}
