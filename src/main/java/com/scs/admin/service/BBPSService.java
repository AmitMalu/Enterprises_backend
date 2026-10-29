package com.scs.admin.service;

import com.scs.admin.dto.*;
import com.scs.admin.response.BillRechargeCheckStatusResponseDto;
import com.scs.admin.response.CommonResponse;

import java.util.List;

public interface BBPSService {

    CommonResponse<List<ProviderListResponseDTO.ProviderDTO>> getProviderList(ProviderListRequestDTO requestDTO);

    FetchBillDetailsResponseDTO fetchBillDetails(FetchBillDetailsRequestDTO requestDTO);

    CommonResponse<?> processRecharge(BillRechargeRequestDTO request,Double latitude, Double longitude, String ipAddress);

    CommonResponse<FetchReceiptResponseDto> fetchReceipt(String  txnId);

}
