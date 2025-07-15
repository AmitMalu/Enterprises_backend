package com.im.admin.dto;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CityResponseDTO {

    private Long id;
    private String cityName;
    private String cityCode;
    private String countryName;
    private String stateName;
    private String districtName;
    private Boolean blocked;

}

