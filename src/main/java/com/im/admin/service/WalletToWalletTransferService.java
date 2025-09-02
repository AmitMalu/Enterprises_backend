package com.im.admin.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.im.admin.dto.WalletToWalletTransferDto;
import com.im.admin.response.CommonResponse;

public interface WalletToWalletTransferService {

    CommonResponse<String> transferAmountWalletToWallet(WalletToWalletTransferDto request) throws JsonProcessingException;
}
