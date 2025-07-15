package com.im.admin.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PinCodeLocationResponse {

    private String pinCode;
    private String countryName;
    private String stateName;
    private String districtName;
    private String cityName;

}

