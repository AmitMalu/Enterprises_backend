package com.scs.admin.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class VendorRemitterEkycRequest {

    @JsonProperty("mobile")
    private String mobile;

    @JsonProperty("lat")
    private String lat;

    @JsonProperty("long")
    private String longitude;

    @JsonProperty("aadhaar_number")
    private String aadhaarNumber;

    @JsonProperty("data")
    private String data;

    @JsonProperty("is_iris")
    private Integer isIris;
}
