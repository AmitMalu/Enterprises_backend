package com.im.admin.service;

import com.im.admin.entities.Distributor;
import com.im.admin.entities.Retailer;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

import java.util.Optional;

public interface RetailerService {

    Retailer createRetailer(Retailer retailer, MultipartFile aadhaarCardFrontImage, MultipartFile aadhaarCardBackImage, MultipartFile panCardImage, MultipartFile shopImage, MultipartFile businessProofImage) throws IOException;

    Retailer updateRetailer(String id, Retailer retailer, MultipartFile aadhaarCardFrontImage, MultipartFile aadhaarCardBackImage, MultipartFile panCardImage, MultipartFile shopImage, MultipartFile businessProofImage) throws IOException;

    void deleteRetailer(String retailerId);

    List<Retailer> getAllRetailers();

    Optional<Retailer> getRetailerById(String retailerId);
}


