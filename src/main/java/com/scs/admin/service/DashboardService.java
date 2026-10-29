package com.scs.admin.service;

import com.scs.admin.dto.DashboardResponseDto;
import com.scs.admin.dto.WalletBalanceResponseDto;
import com.scs.admin.entities.UserDetails;
import com.scs.admin.request.DashboardTransactionRequest;
import com.scs.admin.response.DashboardTransactionDto;
import com.scs.admin.response.MyProfileResponseDto;

import java.util.List;
import java.util.Map;

public interface DashboardService {

    DashboardResponseDto getAdminDashboard();

    WalletBalanceResponseDto getWalletBalances(String hierarchyId);

    MyProfileResponseDto getUserProfile(String hierarchyId);

    Map<String, Object> getDashboardTransactions(DashboardTransactionRequest request);

}
