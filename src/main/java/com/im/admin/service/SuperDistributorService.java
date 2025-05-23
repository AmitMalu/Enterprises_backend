package com.im.admin.service;

import com.im.admin.entities.Distributor;
import com.im.admin.entities.SuperDistributor;
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

    SuperDistributor getSuperDistributor(String id);

    List<SuperDistributor> getAllSuperDistributors();

    void deleteSuperDistributor(String id);
}