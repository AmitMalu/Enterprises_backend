package com.im.admin.service;

import com.im.admin.dto.Aeps2FACheckStatusRequest;
import com.im.admin.entities.AepsTwoFa;
import com.im.admin.request.Aeps2FaRequest;
import com.im.admin.request.AepsTwoFaStatusDto;
import com.im.admin.response.Aeps2FaResponseDto;
import com.im.admin.response.CommonResponse;
import org.springframework.http.ResponseEntity;

public interface AepsTwoFactorService {


    ResponseEntity<CommonResponse<Aeps2FaResponseDto>> validateAadhar(Aeps2FaRequest req);


    ResponseEntity<CommonResponse<AepsTwoFaStatusDto>> check2FAStatus(Aeps2FACheckStatusRequest req);
}
