package com.im.admin.service;

import com.im.admin.dto.KYCRequestDto;
import com.im.admin.response.KYCResponseDto;

public interface KYCService {

    KYCResponseDto getKYCDetails(KYCRequestDto kycRequestDto);
}
