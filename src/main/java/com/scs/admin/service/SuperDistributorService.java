package com.scs.admin.service;

import com.scs.admin.entities.Distributor;
import com.scs.admin.entities.SuperDistributor;
import com.scs.admin.response.SuperDistributorResponseDTO;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface SuperDistributorService {

    SuperDistributor saveSuperDistributor(SuperDistributor superDistributor,
                                     MultipartFile aadhaarCardFrontImage,
                                     MultipartFile aadhaarCardBackImage,
                                     MultipartFile panCardImage,
                                     MultipartFile shopImage,
                                     MultipartFile businessProofImage) throws IOException;

    SuperDistributor updateSuperDistributor(String id,
                                  SuperDistributor superDistributor, MultipartFile aadhaarCardFrontImage,
                                  MultipartFile aadhaarCardBackImage, MultipartFile panCardImage,
                                  MultipartFile shopImage, MultipartFile businessProofImage) throws IOException;

    SuperDistributorResponseDTO getSuperDistributor(String id);

    List<SuperDistributor> getAllSuperDistributors();

    void deleteSuperDistributor(String id);
}