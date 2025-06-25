package com.im.admin.service;

import com.im.admin.dto.WalletLoadRequestDto;
import com.im.admin.entities.WalletLoadRequest;
import com.im.admin.response.CommonResponse;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface WalletLoadRequestService {

    CommonResponse<WalletLoadRequest> saveWalletLoadRequest(WalletLoadRequestDto dto, MultipartFile imageFile) throws IOException;

}
