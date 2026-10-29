package com.scs.admin.service.impl;

import com.scs.admin.entities.Retailer;
import com.scs.admin.repositories.RetailerRepository;
import com.scs.admin.request.GeoLocationRequest;
import com.scs.admin.response.GeoLocationResponse;
import com.scs.admin.service.GeoLocationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class GeoLocationServiceImpl implements GeoLocationService {

    private static final double MAX_DISTANCE_KM = 20.0;
    private static final double EARTH_RADIUS_KM = 6371;

    @Autowired
    RetailerRepository retailerRepository;


    @Override
    public GeoLocationResponse checkRetailerLocation(GeoLocationRequest request) {

        Retailer retailer = retailerRepository.findById(request.getRetailerId())
                .orElseThrow(() -> new RuntimeException("Retailer not found"));

        double dbLat = Double.parseDouble(retailer.getLatitude());
        double dbLon = Double.parseDouble(retailer.getLongitude());

        double inputLat = request.getLatitude();
        double inputLon = request.getLongitude();

        double distance = calculateDistance(dbLat, dbLon, inputLat, inputLon);

        if (distance <= MAX_DISTANCE_KM) {
            return new GeoLocationResponse(
                    true,
                    distance,
                    "Location verified."
            );
        } else {
            return new GeoLocationResponse(
                    false,
                    distance,
                    String.format(
                            "Location verification failed. Your distance is %.0f km away from the service area.",
                            distance
                    )
            );
        }
    }


        private double calculateDistance(
            double lat1, double lon1,
            double lat2, double lon2) {

        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1))
                * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2)
                * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_KM * c;
    }
}
