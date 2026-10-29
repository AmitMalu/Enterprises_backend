package com.scs.admin.service;

import com.scs.admin.dto.Aeps2FACheckStatusRequest;
import com.scs.admin.entities.AepsTwoFa;
import com.scs.admin.request.Aeps2FaRequest;
import com.scs.admin.request.AepsTwoFaStatusDto;
import com.scs.admin.response.Aeps2FaResponseDto;
import com.scs.admin.response.CommonResponse;
import org.springframework.http.ResponseEntity;

public interface AepsTwoFactorService {


    ResponseEntity<CommonResponse<Aeps2FaResponseDto>> validateAadhar(Aeps2FaRequest req);


    ResponseEntity<CommonResponse<AepsTwoFaStatusDto>> check2FAStatus(Aeps2FACheckStatusRequest req);
}
