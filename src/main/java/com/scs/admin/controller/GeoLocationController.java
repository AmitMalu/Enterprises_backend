package com.scs.admin.controller;

import com.scs.admin.request.GeoLocationRequest;
import com.scs.admin.response.CommonResponse;
import com.scs.admin.response.GeoLocationResponse;
import com.scs.admin.service.GeoLocationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/geo")
public class GeoLocationController {

    @Autowired
    private GeoLocationService geoLocationService;

    @PostMapping("/check")
    public ResponseEntity<CommonResponse<GeoLocationResponse>> checkGeoLocation(
            @RequestBody GeoLocationRequest request) {

        GeoLocationResponse geoResponse = geoLocationService.checkRetailerLocation(request);

        if (geoResponse.isAccepted()) {
            return ResponseEntity.ok(
                    CommonResponse.success(
                            geoResponse,
                            geoResponse.getMessage()
                    )
            );
        } else {
            return ResponseEntity.ok(
                    CommonResponse.failedResponse(
                            HttpStatus.BAD_REQUEST.value(),
                            geoResponse.getMessage(),
                            geoResponse
                    )
            );
        }
    }

}
