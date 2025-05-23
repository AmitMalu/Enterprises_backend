package com.im.admin.service;

import com.im.admin.entities.Distributor;
import com.im.admin.entities.Partner;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface PartnerService {

    Partner savePartner(Partner partner, MultipartFile aadhaarCardFrontImage, MultipartFile aadhaarCardBackImage, MultipartFile panCardImage, MultipartFile shopImage, MultipartFile businessProofImage) throws IOException;

    Partner updatePartner(String partnerId, Partner partner, MultipartFile aadhaarCardFrontImage, MultipartFile aadhaarCardBackImage, MultipartFile panCardImage, MultipartFile shopImage, MultipartFile businessProofImage) throws IOException;

    List<Partner> getAllPartners();

    Partner getPartnerById(String partnerId);

    void deletePartner(String partnerId);
}








