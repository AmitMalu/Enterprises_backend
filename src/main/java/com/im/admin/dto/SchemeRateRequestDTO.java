package com.im.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SchemeRateRequestDTO {

    private String schemeCode;
    private String operatorName;
    private String operatorCode;
    private List<SchemeRateSlabDTO> slabs; // List of slabs
}

