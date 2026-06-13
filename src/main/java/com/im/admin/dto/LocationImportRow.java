package com.im.admin.dto;

import lombok.Data;

/**
 * @author SHUBHAM KHOPADE
 */
@Data
public class LocationImportRow {

    private String country;
    private String countryCode;

    private String state;
    private String stateCode;
    private String regionType;

    private String district;
    private String districtCode;

    private String city;
    private String cityCode;

    private String pinCode;

}
