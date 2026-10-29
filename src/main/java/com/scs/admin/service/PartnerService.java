package com.scs.admin.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.scs.admin.entities.Distributor;
import com.scs.admin.entities.Partner;
import com.scs.admin.response.PartnerResponseDTO;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface PartnerService {

    Partner savePartner(Partner partner, MultipartFile aadhaarCardFrontImage, MultipartFile aadhaarCardBackImage, MultipartFile panCardImage, MultipartFile shopImage, MultipartFile businessProofImage) throws IOException;

    Partner updatePartner(String partnerId, Partner partner, MultipartFile aadhaarCardFrontImage, MultipartFile aadhaarCardBackImage, MultipartFile panCardImage, MultipartFile shopImage, MultipartFile businessProofImage) throws IOException;

    List<Partner> getAllPartners() throws JsonProcessingException;

    PartnerResponseDTO getPartnerById(String partnerId) throws JsonProcessingException;

    void deletePartner(String partnerId);
}








