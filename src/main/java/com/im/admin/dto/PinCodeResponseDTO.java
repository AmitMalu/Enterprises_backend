package com.im.admin.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PinCodeResponseDTO {

    private Long id;
    private String pinCode;
    private String countryName;
    private String stateName;
    private String districtName;
    private String cityName;
    private Boolean blocked;

}

