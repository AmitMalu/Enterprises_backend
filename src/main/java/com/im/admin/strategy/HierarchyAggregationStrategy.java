package com.im.admin.strategy;

import com.im.admin.response.HierarchyCountResponse;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface HierarchyAggregationStrategy {

    BigDecimal getCommission(
            String hierarchyId,
            LocalDate fromDate,
            LocalDate toDate
    );

    BigDecimal getTransactionVolume(
            String hierarchyId,
            LocalDate fromDate,
            LocalDate toDate
    );

    Long getPendingTransactionCount(String id, LocalDate from, LocalDate to);

    HierarchyCountResponse getRetailerCount(String id);

    List<Map<String, Object>> getLast4MonthsGraphData(String id);

    BigDecimal getFundTransferred(
            String hierarchyId,
            LocalDate fromDate,
            LocalDate toDate
    );

    BigDecimal getFundReceived(
            String hierarchyId,
            LocalDate fromDate,
            LocalDate toDate
    );

    List<Map<String, Object>> getServicePieData(String hierarchyId);


}
