package com.im.admin.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.im.admin.dto.WalletToWalletTransferDto;

public interface WalletToWalletTransferService {

    void transferAmountWalletToWallet(WalletToWalletTransferDto request, Double latitude, Double longitude) throws JsonProcessingException;

}
