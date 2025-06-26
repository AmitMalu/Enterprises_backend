package com.im.admin.service;

import com.im.admin.dto.FundRequestActionDto;
import com.im.admin.dto.PrefundRequestDto;
import com.im.admin.entities.PrefundRequest;
import com.im.admin.response.CommonResponse;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface PrefundRequestService {

    CommonResponse<PrefundRequest> saveWalletLoadRequest(PrefundRequestDto dto, MultipartFile imageFile) throws IOException;

    Page<PrefundRequest> getAllPrefundRequests(int page, int size);

    CommonResponse<String> takeActionOnFundRequest(FundRequestActionDto actionDto);

    CommonResponse<PrefundRequest> getById(Long id);



}
