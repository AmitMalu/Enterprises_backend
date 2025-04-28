package com.im.admin.service;

import com.im.admin.dto.WalletToWalletTransferDto;

public interface WalletToWalletTransferService {

    void transferAmountWalletToWallet(WalletToWalletTransferDto request);
}
