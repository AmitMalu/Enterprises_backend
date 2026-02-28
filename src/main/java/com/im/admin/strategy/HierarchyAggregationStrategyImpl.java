package com.im.admin.strategy;

import com.im.admin.entities.UserDetails;
import com.im.admin.repositories.*;
import com.im.admin.response.HierarchyCountResponse;
import com.im.admin.util.CommonMethods;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class HierarchyAggregationStrategyImpl implements HierarchyAggregationStrategy{

    @Autowired
    private CommonMethods commonMethods;

    @Autowired
    private UserDetailsRepository userDetailsRepository;

    @Autowired
    private FundTransferRepository fundTransferRepository;

    @Autowired
    private FundRequestRepository fundRequestRepository;

    @Autowired
    private SuperDistributorPostpaidTransactionDetailsRepository sdPostpaidTransactionDetailsRepository;

    @Autowired
    private SuperDistributorPrepaidTransactionDetailsRepository sdPrepaidTransactionDetailsRepository;

    @Autowired
    private PartnerPrepaidTransactionDetailsRepository ptPrepaidTransactionDetailsRepository;

    @Autowired
    private PartnerPostpaidTransactionDetailsRepository ptPostpaidTransactionDetailsRepository;

    @Autowired
    private DistributorPostpaidTransactionDetailsRepository dtPostpaidTransactionDetailsRepository;

    @Autowired
    private DistributorPrepaidTransactionDetailsRepository dtPrepaidTransactionDetailsRepository;

    @Autowired
    private RetailerPrepaidTransactionDetailsRepository retailerPrepaidTransactionDetailsRepository;

    @Autowired
    private RetailerPostpaidTransactionDetailsRepository retailerPostpaidTransactionDetailsRepository;

    @Override
    public BigDecimal getCommission(String hierarchyId, LocalDate fromDate, LocalDate toDate) {
        BigDecimal prepaid = BigDecimal.ZERO;
        BigDecimal postpaid = BigDecimal.ZERO;

        String type = commonMethods.extractHierarchyType(
                hierarchyId
        );

        switch (type) {

            case "SD" -> {
                prepaid = sdPrepaidTransactionDetailsRepository
                        .sumCommission(hierarchyId, fromDate, toDate);

                postpaid = sdPostpaidTransactionDetailsRepository
                        .sumCommission(hierarchyId, fromDate, toDate);
            }

            case "PT" -> {
                prepaid = ptPrepaidTransactionDetailsRepository
                        .sumCommission(hierarchyId, fromDate, toDate);

                postpaid = ptPostpaidTransactionDetailsRepository
                        .sumCommission(hierarchyId, fromDate, toDate);
            }

            case "DT" -> {
                prepaid = dtPrepaidTransactionDetailsRepository
                        .sumCommission(hierarchyId, fromDate, toDate);

                postpaid = dtPostpaidTransactionDetailsRepository
                        .sumCommission(hierarchyId, fromDate, toDate);
            }

            case "RT" -> {
                prepaid = retailerPrepaidTransactionDetailsRepository
                        .sumCommissionEarned(hierarchyId, fromDate, toDate);

                postpaid = retailerPostpaidTransactionDetailsRepository
                        .sumCommissionEarned(hierarchyId, fromDate, toDate);
            }
        }

        return prepaid.add(postpaid);
    }

    @Override
    public BigDecimal getTransactionVolume(String hierarchyId, LocalDate fromDate, LocalDate toDate) {
        return null;
    }

    @Override
    public Long getPendingTransactionCount(String id, LocalDate from, LocalDate to) {
        return 0L;
    }

    @Override
    public HierarchyCountResponse getRetailerCount(String id) {
        List<UserDetails> allChildren = new ArrayList<>();

        fetchChildrenRecursively(id, allChildren);

        // Dynamic grouping by hierarchyCode
        Map<String, Long> countMap = allChildren.stream()
                .collect(Collectors.groupingBy(
                        UserDetails::getHierarchyCode,
                        Collectors.counting()
                ));

        long totalCount = countMap.values()
                .stream()
                .mapToLong(Long::longValue)
                .sum();

        HierarchyCountResponse response = new HierarchyCountResponse();
        response.setHierarchyId(id);
        response.setCounts(countMap);
        response.setTotalCount(totalCount);

        return response;
    }

    private void fetchChildrenRecursively(
            String parentId,
            List<UserDetails> result
    ) {

        List<UserDetails> children =
                userDetailsRepository.findByParentAccount(parentId);

        for (UserDetails child : children) {
            result.add(child);
            fetchChildrenRecursively(child.getHierarchyId(), result);
        }
    }


    @Override
    public List<Map<String, Object>> getLast4MonthsGraphData(String id) {
        return List.of();
    }

    public BigDecimal getFundTransferred(String hierarchyId,
                                         LocalDate fromDate,
                                         LocalDate toDate) {

        LocalDateTime fromDateTime = null;
        LocalDateTime toDateTime = null;

        if (fromDate != null) {
            fromDateTime = fromDate.atStartOfDay(); // 00:00:00
        }

        if (toDate != null) {
            toDateTime = toDate.atTime(23, 59, 59); // end of day
        }

        return fundTransferRepository.sumFundTransferred(
                hierarchyId,
                fromDateTime,
                toDateTime
        );
    }

    @Override
    public BigDecimal getFundReceived(String hierarchyId, LocalDate fromDate, LocalDate toDate) {
        return fundRequestRepository.sumFundReceived(hierarchyId, fromDate, toDate);
    }

    @Override
    public List<Map<String, Object>> getServicePieData(String hierarchyId) {
        return List.of();
    }
}
