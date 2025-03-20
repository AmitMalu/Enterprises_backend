package com.im.admin.service;

import com.im.admin.entities.Retailer;

import java.util.List;

import java.util.Optional;

public interface RetailerService {

    Retailer createRetailer(Retailer retailer);

    Retailer updateRetailer(String retailerId, Retailer retailerDetails);

    void deleteRetailer(String retailerId);

    List<Retailer> getAllRetailers();

    Optional<Retailer> getRetailerById(String retailerId);
}


