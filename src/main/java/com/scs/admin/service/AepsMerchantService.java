package com.scs.admin.service;

import com.scs.admin.request.AepsCheckStatus;
import com.scs.admin.request.AepsMerchantRequestDto;
import com.scs.admin.request.AepsStatusDataDto;
import com.scs.admin.response.AepsMerchantResponseDto;
import com.scs.admin.response.AepsStatusCheckResponse;
import com.scs.admin.response.CommonResponse;
import org.springframework.http.ResponseEntity;

public interface AepsMerchantService {
    CommonResponse<AepsMerchantResponseDto> registerMerchantAndCall(AepsMerchantRequestDto req);

    CommonResponse<AepsStatusDataDto> checkStatus(AepsCheckStatus aepsCheckStatus);

}
