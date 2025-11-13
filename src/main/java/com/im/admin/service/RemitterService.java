package com.im.admin.service;

import com.im.admin.dto.RemitterEkycRequest;
import com.im.admin.dto.RemitterQueryResponse;
import com.im.admin.dto.RemitterRegisterRequest;
import com.im.admin.dto.RemitterRegisterResponse;
import com.im.admin.entities.DmtRemitter;
import com.im.admin.response.CommonResponse;

public interface RemitterService {

    CommonResponse<RemitterQueryResponse> checkRemitter(String mobile,String subProductId);

    CommonResponse<RemitterRegisterResponse> registerRemitter(RemitterRegisterRequest request);

    CommonResponse<Object> doEkyc(RemitterEkycRequest request);

    CommonResponse<DmtRemitter> fetchRemitterDetails(String mobileNumber);

}
