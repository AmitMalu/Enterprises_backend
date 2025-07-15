package com.im.admin.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DistrictResponseDTO {

    private Long id;
    private String districtName;
    private String districtCode;
    private String countryName;
    private Long countryId;
    private String stateName;
    private Long stateId;
    private Boolean blocked;
}

