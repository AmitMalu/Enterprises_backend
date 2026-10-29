package com.scs.admin.service;

import com.scs.admin.request.GeoLocationRequest;
import com.scs.admin.response.GeoLocationResponse;

public interface GeoLocationService {
    GeoLocationResponse checkRetailerLocation(GeoLocationRequest request);
}
