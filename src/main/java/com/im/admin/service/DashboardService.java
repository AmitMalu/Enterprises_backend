package com.im.admin.service;

import com.im.admin.dto.DashboardResponseDto;
import com.im.admin.dto.WalletBalanceResponseDto;

public interface DashboardService {

    DashboardResponseDto getAdminDashboard();

    WalletBalanceResponseDto getWalletBalances(String hierarchyId);
}
