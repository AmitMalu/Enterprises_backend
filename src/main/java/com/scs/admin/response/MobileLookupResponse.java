package com.scs.admin.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class MobileLookupResponse {

    private Integer statusCode;
    private String msg;

    @JsonProperty("lookup_number")
    private String lookupNumber;

    private String operator;
    private String circle;

    private Integer spkey;
    private String circleCode;

    private Boolean isCircleOnly;
    private Integer deliveryStatus;
}
