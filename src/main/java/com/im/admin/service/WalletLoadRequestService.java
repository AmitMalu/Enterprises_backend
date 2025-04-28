package com.im.admin.service;

import com.im.admin.dto.WalletLoadRequestDto;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface WalletLoadRequestService {

    String saveWalletLoadRequest(WalletLoadRequestDto dto, MultipartFile imageFile) throws IOException;

}
