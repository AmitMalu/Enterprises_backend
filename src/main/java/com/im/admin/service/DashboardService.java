package com.im.admin.service;

import com.im.admin.dto.DashboardResponseDto;
import com.im.admin.dto.WalletBalanceResponseDto;
import com.im.admin.entities.UserDetails;
import com.im.admin.request.DashboardTransactionRequest;
import com.im.admin.response.DashboardTransactionDto;
import com.im.admin.response.MyProfileResponseDto;

import java.util.List;
import java.util.Map;

public interface DashboardService {

    DashboardResponseDto getAdminDashboard();

    WalletBalanceResponseDto getWalletBalances(String hierarchyId);

    MyProfileResponseDto getUserProfile(String hierarchyId);

    Map<String, Object> getDashboardTransactions(DashboardTransactionRequest request);

}
