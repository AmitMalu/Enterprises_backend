package com.im.admin.service;

import com.im.admin.request.GeoLocationRequest;
import com.im.admin.response.GeoLocationResponse;

public interface GeoLocationService {
    GeoLocationResponse checkRetailerLocation(GeoLocationRequest request);
}
