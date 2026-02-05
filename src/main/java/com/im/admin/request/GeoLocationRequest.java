package com.im.admin.request;

import lombok.Data;

@Data
public class GeoLocationRequest {

    private String retailerId;
    private Double latitude;
    private Double longitude;
}
