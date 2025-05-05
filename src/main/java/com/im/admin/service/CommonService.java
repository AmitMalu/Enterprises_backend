package com.im.admin.service;

import com.im.admin.response.WalletBalanceResponseDto;

public interface CommonService {

    WalletBalanceResponseDto fetchWalletBalance(String hierarchyId);

}
