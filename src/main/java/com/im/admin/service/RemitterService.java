package com.im.admin.service;

import com.im.admin.dto.*;
import com.im.admin.request.RemitterUpdateRequest;
import com.im.admin.entities.DmtRemitter;
import com.im.admin.request.DmtOtpRequest;
import com.im.admin.response.CommonResponse;
import com.im.admin.response.DmtRemitterResponseDto;

public interface RemitterService {

    CommonResponse<RemitterQueryResponse> checkRemitter(String mobile,String subProductId) throws Exception ;

    CommonResponse<RemitterRegisterResponse> registerRemitter(RemitterRegisterRequest request);

    CommonResponse<Object> doEkyc(RemitterEkycRequest request);

    CommonResponse<DmtRemitterResponseDto> fetchRemitterDetails(String mobileNumber);

    CommonResponse<DmtRemitterResponseDto> updateRemitter(RemitterUpdateRequest request);

    CommonResponse<String> sendOtp(DmtOtpRequest dmtOtpRequest);

    CommonResponse<String> resendOtp(String mobile);
}
