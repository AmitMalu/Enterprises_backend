package com.scs.admin.request;

import lombok.Data;

@Data
public class GeoLocationRequest {

    private String retailerId;
    private Double latitude;
    private Double longitude;
}
