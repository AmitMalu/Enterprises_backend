package com.scs.admin.service;

import com.scs.admin.dto.*;
import com.scs.admin.request.RemitterUpdateRequest;
import com.scs.admin.entities.DmtRemitter;
import com.scs.admin.request.DmtOtpRequest;
import com.scs.admin.response.CommonResponse;
import com.scs.admin.response.DmtRemitterResponseDto;

public interface RemitterService {

    CommonResponse<RemitterQueryResponse> checkRemitter(String mobile,String subProductId) throws Exception ;

    CommonResponse<RemitterRegisterResponse> registerRemitter(RemitterRegisterRequest request);

    CommonResponse<Object> doEkyc(RemitterEkycRequest request);

    CommonResponse<DmtRemitterResponseDto> fetchRemitterDetails(String mobileNumber);

    CommonResponse<DmtRemitterResponseDto> updateRemitter(RemitterUpdateRequest request);

//    CommonResponse<String> sendOtp(DmtOtpRequest dmtOtpRequest);

    CommonResponse<String> sendOtpWithShiv(DmtOtpRequest dmtOtpRequest);

//    CommonResponse<String> resendOtp(String mobile);

    CommonResponse<String> resendOtpWithShiv(String mobile);
}
