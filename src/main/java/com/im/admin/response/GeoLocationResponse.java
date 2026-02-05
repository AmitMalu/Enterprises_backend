package com.im.admin.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class GeoLocationResponse {

    private boolean accepted;
    private double distanceInKm;
    private String message;
}
