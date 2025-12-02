package com.im.admin.service;

import com.im.admin.request.AepsCheckStatus;
import com.im.admin.request.AepsMerchantRequestDto;
import com.im.admin.request.AepsStatusDataDto;
import com.im.admin.response.AepsMerchantResponseDto;
import com.im.admin.response.AepsStatusCheckResponse;
import com.im.admin.response.CommonResponse;
import org.springframework.http.ResponseEntity;

public interface AepsMerchantService {
    CommonResponse<AepsMerchantResponseDto> registerMerchantAndCall(AepsMerchantRequestDto req);

    CommonResponse<AepsStatusDataDto> checkStatus(AepsCheckStatus aepsCheckStatus);

}
