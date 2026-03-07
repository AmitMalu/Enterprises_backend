package com.im.admin.service;


import com.im.admin.entities.Distributor;
import com.im.admin.response.DistributorResponseDTO;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface DistributorService {

    Distributor createDistributor(Distributor distributor, MultipartFile aadhaarCardFrontImage, MultipartFile aadhaarCardBackImage, MultipartFile panCardImage, MultipartFile shopImage, MultipartFile businessProofImage) throws IOException;

    Distributor updateDistributor(String id,
                                  Distributor distributor, MultipartFile aadhaarCardFrontImage,
                                  MultipartFile aadhaarCardBackImage, MultipartFile panCardImage,
                                  MultipartFile shopImage, MultipartFile businessProofImage) throws IOException;
    List<Distributor> getAllDistributors();

    DistributorResponseDTO getDistributorById(String id);

    void deleteDistributor(String id);
}


