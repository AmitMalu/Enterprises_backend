package com.im.admin.service;

import com.im.admin.dto.DashboardResponseDto;
import com.im.admin.dto.WalletBalanceResponseDto;
import com.im.admin.entities.UserDetails;
import com.im.admin.response.MyProfileResponseDto;

public interface DashboardService {

    DashboardResponseDto getAdminDashboard();

    WalletBalanceResponseDto getWalletBalances(String hierarchyId);

    MyProfileResponseDto getUserProfile(String hierarchyId);
}
