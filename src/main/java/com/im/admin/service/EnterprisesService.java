package com.im.admin.service;

import com.im.admin.entities.Enterprises;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface EnterprisesService {

    Enterprises saveEnterprise(Enterprises enterprise,
                               MultipartFile aadhaarCardFrontImage,
                               MultipartFile aadhaarCardBackImage,
                               MultipartFile panCardImage,
                               MultipartFile shopImage,
                               MultipartFile businessProofImage) throws IOException;

    Enterprises updateEnterprise(String id,
                                 Enterprises enterprise, MultipartFile aadhaarCardFrontImage,
                                 MultipartFile aadhaarCardBackImage, MultipartFile panCardImage,
                                 MultipartFile shopImage, MultipartFile businessProofImage) throws IOException;

    List<Enterprises> getAllEnterprises();

    Enterprises getEnterpriseById(String id);

    void deleteEnterprise(String id);
}



