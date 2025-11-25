package com.im.admin.service;

import com.im.admin.request.AepsCheckStatus;
import com.im.admin.request.AepsMerchantRequestDto;
import com.im.admin.response.AepsMerchantResponseDto;
import com.im.admin.response.AepsStatusCheckResponse;
import org.springframework.http.ResponseEntity;

public interface AepsMerchantService {
    ResponseEntity<AepsMerchantResponseDto> registerMerchantAndCall(AepsMerchantRequestDto aepsMerchantRequestDto);

    AepsStatusCheckResponse checkStatus(AepsCheckStatus aepsCheckStatus);
}
