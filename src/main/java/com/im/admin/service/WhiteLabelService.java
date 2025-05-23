package com.im.admin.service;

import com.im.admin.entities.Distributor;
import com.im.admin.entities.WhiteLabels;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface WhiteLabelService {

    WhiteLabels saveWhiteLabel(WhiteLabels distributor,
                               MultipartFile aadhaarCardFrontImage,
                               MultipartFile aadhaarCardBackImage,
                               MultipartFile panCardImage,
                               MultipartFile shopImage,
                               MultipartFile businessProofImage) throws IOException;

    WhiteLabels updateWhiteLabel(String id,
                                  WhiteLabels distributor, MultipartFile aadhaarCardFrontImage,
                                  MultipartFile aadhaarCardBackImage, MultipartFile panCardImage,
                                  MultipartFile shopImage, MultipartFile businessProofImage) throws IOException;

    List<WhiteLabels> getAllWhiteLabels();

    WhiteLabels getWhiteLabelById(String id);

    void deleteWhiteLabel(String id);
}