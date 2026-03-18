package com.im.admin.service;

import com.im.admin.dto.FundRequestActionDto;
import com.im.admin.dto.PrefundRequestDto;
import com.im.admin.entities.PrefundRequest;
import com.im.admin.response.CommonResponse;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;

public interface PrefundRequestService {

    CommonResponse<PrefundRequest> saveWalletLoadRequest(PrefundRequestDto dto, MultipartFile imageFile) throws IOException;

    Page<PrefundRequest> getAllPrefundRequests(int page, int size,
                                               Long id, String requestedBy, String mobileNumber, Double depositAmount,
                                               String status, LocalDate depositDate,   LocalDate fromDate,
                                               LocalDate toDate,LocalDate createDateTime,
                                               String bankHolderName, String bankAccountName, String bankAccountNumber,
                                               String bankTranId, String paymentMode, String narration, String ipAddress,
                                               LocalDate approveOrRejectDate, String approveOrRejectTime, String remarks, String depositType);


    CommonResponse<String> takeActionOnFundRequest(FundRequestActionDto actionDto, Double latitude, Double longitude);

    CommonResponse<PrefundRequest> getById(Long id);



}
