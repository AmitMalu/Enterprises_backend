package com.scs.admin.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PinCodeLocationResponse {

    private String pinCode;
    private String countryName;
    private Long countryId;
    private String stateName;
    private Long stateId;
    private String districtName;
    private Long districtId;
    private String cityName;
    private Long cityId;


}

