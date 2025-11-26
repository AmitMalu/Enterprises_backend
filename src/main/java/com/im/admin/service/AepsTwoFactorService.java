package com.im.admin.service;

import com.im.admin.request.Aeps2FaRequest;
import com.im.admin.response.Aeps2FaResponseDto;
import org.springframework.http.ResponseEntity;

public interface AepsTwoFactorService {


    ResponseEntity<Aeps2FaResponseDto> validateAadhar(Aeps2FaRequest req);
}
