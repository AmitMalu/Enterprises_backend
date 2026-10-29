package com.scs.admin.dto;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CityRequestDTO {

    private String cityName;
    private String cityCode;
    private Long countryId;
    private Long stateId;
    private Long districtId;
    private Boolean blocked;
    private String createdBy;
    private String editedBy;

}

