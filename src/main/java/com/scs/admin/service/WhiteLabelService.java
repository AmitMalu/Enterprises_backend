package com.scs.admin.service;

import com.scs.admin.dto.WhiteLabelDropdownDTO;
import com.scs.admin.entities.Distributor;
import com.scs.admin.entities.WhiteLabels;
import com.scs.admin.response.WhiteLabelResponseDTO;
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

    WhiteLabelResponseDTO getWhiteLabelById(String id);

    void deleteWhiteLabel(String id);

    List<WhiteLabelDropdownDTO> getAdminWhiteLabels();
}