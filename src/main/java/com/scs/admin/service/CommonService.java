package com.scs.admin.service;

import com.scs.admin.response.WalletBalanceResponseDto;

public interface CommonService {

    WalletBalanceResponseDto fetchWalletBalance(String hierarchyId);

}
