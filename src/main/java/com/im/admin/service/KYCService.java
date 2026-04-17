package com.im.admin.service;

import com.im.admin.dto.KYCEditRequest;
import com.im.admin.dto.KYCRequestDto;
import com.im.admin.response.KYCResponseDto;
import com.im.admin.response.KycDocumentDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface KYCService {

    KYCResponseDto searchByMobileNoAndHierarchyId(KYCRequestDto kycRequestDto);

    Page<KYCResponseDto> searchByHierarchyWise(String hierarchyType, Pageable pageable);

    String editKyc(KYCEditRequest request, MultipartFile imageFile);

    void deleteKycDocument(String hierarchyId, String category);

    Page<Object> getApprovedDocuments(String searchKeyword, Pageable pageable);

    List<KycDocumentDto> getKycDetailDocumentsByHierarchyId(String hierarchyId);

}
