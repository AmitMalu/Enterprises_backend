package com.im.admin.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class RemitterEkycRequest {

    private String mobile;          // Remitter mobile number
    private String lat;             // Latitude
//    private String long;             // Longitude (or "lon")

    @JsonProperty("long")
    private String longitude;
    private String aadhaar_number;  // Aadhaar number
    private String data;            // Encrypted PID XML
    private String accessmode = "WEB";
    private Integer is_iris = 2;    // 2 = Finger default
    private String subProductId;
    private String name;
}
