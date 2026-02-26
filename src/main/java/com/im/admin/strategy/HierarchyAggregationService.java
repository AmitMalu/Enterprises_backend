package com.im.admin.strategy;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class HierarchyAggregationService {

    private final Map<String, HierarchyAggregationStrategy> strategies;

    public BigDecimal getCommission(
            String hierarchyId,
            String hierarchyType,
            LocalDate fromDate,
            LocalDate toDate
    ) {
        return getStrategy(hierarchyType)
                .getCommission(hierarchyId, fromDate, toDate);
    }

    public BigDecimal getTransactionVolume(
            String hierarchyId,
            String hierarchyType,
            LocalDate fromDate,
            LocalDate toDate
    ) {
        return getStrategy(hierarchyType)
                .getTransactionVolume(hierarchyId, fromDate, toDate);
    }

    public BigDecimal getFundTransferred(
            String hierarchyId,
            String hierarchyType,
            LocalDate fromDate,
            LocalDate toDate
    ) {
        return getStrategy(hierarchyType)
                .getFundTransferred(hierarchyId, fromDate, toDate);
    }

    public BigDecimal getFundReceived(
            String hierarchyId,
            String hierarchyType,
            LocalDate fromDate,
            LocalDate toDate
    ) {
        return getStrategy(hierarchyType)
                .getFundReceived(hierarchyId, fromDate, toDate);
    }

    public List<Map<String, Object>> getServicePieData(
            String hierarchyId,
            String hierarchyType
    ) {
        return getStrategy(hierarchyType)
                .getServicePieData(hierarchyId);
    }

    public HierarchyAggregationStrategy getStrategy(String type) {
        HierarchyAggregationStrategy strategy = strategies.get(type);
        if (strategy == null)
            throw new IllegalArgumentException("Invalid type: " + type);
        return strategy;
    }
}
