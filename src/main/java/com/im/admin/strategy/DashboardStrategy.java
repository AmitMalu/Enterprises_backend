package com.im.admin.strategy;

import com.im.admin.request.DashboardTransactionRequest;

import java.time.LocalDate;
import java.util.Map;

public interface DashboardStrategy {

        Map<String, Object> buildDashboard(
                DashboardTransactionRequest request,
                LocalDate fromDate,
                LocalDate toDate
        );

}
