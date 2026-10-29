package com.scs.admin.strategy;

import com.scs.admin.request.DashboardTransactionRequest;

import java.time.LocalDate;
import java.util.Map;

public interface DashboardStrategy {

        Map<String, Object> buildDashboard(
                DashboardTransactionRequest request,
                LocalDate fromDate,
                LocalDate toDate
        );

}
