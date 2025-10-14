package com.im.admin.service;

import com.im.admin.dto.*;
import com.im.admin.response.BillRechargeCheckStatusResponseDto;
import com.im.admin.response.CommonResponse;

import java.util.List;

public interface BBPSService {

    CommonResponse<List<ProviderListResponseDTO.ProviderDTO>> getProviderList(ProviderListRequestDTO requestDTO);

    FetchBillDetailsResponseDTO fetchBillDetails(FetchBillDetailsRequestDTO requestDTO);

    CommonResponse<?> processRecharge(BillRechargeRequestDTO request);

    CommonResponse<FetchReceiptResponseDto> fetchReceipt(String  txnId);

}
