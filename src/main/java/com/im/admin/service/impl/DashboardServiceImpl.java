package com.im.admin.service.impl;


import com.im.admin.dto.DashboardResponseDto;
import com.im.admin.dto.WalletBalanceResponseDto;
import com.im.admin.repositories.DashboardQueryRepository;
import com.im.admin.service.DashboardService;
import com.im.admin.util.CommonMethods;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;

@Service
@Slf4j
public class DashboardServiceImpl implements DashboardService {

    @Autowired
    private DashboardQueryRepository dashboardQueryRepository;

    @Autowired
    private CommonMethods commonMethods;

    @Override
    public DashboardResponseDto getAdminDashboard() {
        Map<String, Object> sums = dashboardQueryRepository.fetchTotalTransactionSums();

        BigDecimal totalPrepaid = sumBigDecimals(
                sums.get("retailer_prepaid"),
                sums.get("distributor_prepaid"),
                sums.get("partner_prepaid"),
                sums.get("super_distributor_prepaid"),
                sums.get("enterprises_prepaid"),
                sums.get("white_label_prepaid")
        );

        BigDecimal totalPostpaid = sumBigDecimals(
                sums.get("retailer_postpaid"),
                sums.get("distributor_postpaid"),
                sums.get("partner_postpaid"),
                sums.get("super_distributor_postpaid"),
                sums.get("enterprises_postpaid"),
                sums.get("white_label_postpaid")
        );

        BigDecimal totalTransaction = totalPrepaid.add(totalPostpaid);

        // Sum of pending prepaid transactions
        BigDecimal pendingPrepaid = sumBigDecimals(
                sums.get("retailer_prepaid_pending"),
                sums.get("distributor_prepaid_pending"),
                sums.get("partner_prepaid_pending"),
                sums.get("super_distributor_prepaid_pending"),
                sums.get("enterprises_prepaid_pending"),
                sums.get("white_label_prepaid_pending")
        );

        // Sum of pending postpaid transactions
        BigDecimal pendingPostpaid = sumBigDecimals(
                sums.get("retailer_postpaid_pending"),
                sums.get("distributor_postpaid_pending"),
                sums.get("partner_postpaid_pending"),
                sums.get("super_distributor_postpaid_pending"),
                sums.get("enterprises_postpaid_pending"),
                sums.get("white_label_postpaid_pending")
        );

        // Total pending transactions (Prepaid + Postpaid)
        BigDecimal pendingTransaction = pendingPrepaid.add(pendingPostpaid);

        //Pending onboarding count
        Long pendingOnboarding = dashboardQueryRepository.fetchPendingOnboarding();

        DashboardResponseDto response = new DashboardResponseDto();
        response.setTotalTransaction(totalTransaction);
        response.setPendingTransaction(pendingTransaction);
        response.setPendingOnboarding(pendingOnboarding);

        return response;
    }

    private BigDecimal sumBigDecimals(Object... values) {
        BigDecimal sum = BigDecimal.ZERO;
        for (Object value : values) {
            if (value instanceof BigDecimal) {
                sum = sum.add((BigDecimal) value);
            } else if (value instanceof Number) {
                sum = sum.add(BigDecimal.valueOf(((Number) value).doubleValue()));
            } else if (value != null) {
                sum = sum.add(new BigDecimal(value.toString()));
            }
        }
        return sum;
    }

    @Override
    public WalletBalanceResponseDto getWalletBalances(String hierarchyId) {
        BigDecimal prepaid = commonMethods.getCurrentBalance(hierarchyId, "PREPAID");
        BigDecimal postpaid =commonMethods.getCurrentBalance(hierarchyId, "POSTPAID");

        return new WalletBalanceResponseDto(prepaid, postpaid);
    }

}
