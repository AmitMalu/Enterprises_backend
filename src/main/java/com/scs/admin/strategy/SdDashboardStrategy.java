package com.scs.admin.strategy;

import com.scs.admin.entities.SuperDistributor;
import com.scs.admin.entities.SuperDistributorPostpaidAmount;
import com.scs.admin.entities.SuperDistributorPrepaidAmount;
import com.scs.admin.repositories.*;
import com.scs.admin.request.DashboardTransactionRequest;
import com.scs.admin.response.DashboardTransactionDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

@Component("SD")
@RequiredArgsConstructor
public class SdDashboardStrategy implements DashboardStrategy {

    private final SuperDistributorRepository superDistributorRepository;
    private final SuperDistributorPrepaidAmountRepository prepaidRepo;
    private final SuperDistributorPostpaidAmountRepository postpaidRepo;

    private final SuperDistributorPrepaidTransactionDetailsRepository superDistributorPrepaidTransactionDetailsRepository;
    private final SuperDistributorPostpaidTransactionDetailsRepository superDistributorPostpaidTransactionDetailsRepository;

    private final HierarchyAggregationService aggregationService;

    @Override
    public Map<String, Object> buildDashboard(
            DashboardTransactionRequest request,
            LocalDate fromDate,
            LocalDate toDate
    ) {

        String sdId = request.getHierarchyId();

        SuperDistributor sd = superDistributorRepository
                .findBySuperDistributorId(sdId)
                .orElseThrow(() ->
                        new RuntimeException("SuperDistributor not found"));

        BigDecimal prepaid = prepaidRepo
                .findBySuperDistributorId_SuperDistributorId(sdId)
                .map(SuperDistributorPrepaidAmount::getAvailableBalance)
                .orElse(BigDecimal.ZERO);

        BigDecimal postpaid = postpaidRepo
                .findBySuperDistributorId_SuperDistributorId(sdId)
                .map(SuperDistributorPostpaidAmount::getAvailableBalance)
                .orElse(BigDecimal.ZERO);

        Map<String, Object> profile = Map.of(
                "image", Optional.ofNullable(sd.getShopImage()).orElse(""),
                "name", sd.getOutletAddress().getOutletName(),
                "superDistributorId", sd.getSuperDistributorId(),
                "prepaid", prepaid,
                "postpaid", postpaid
        );

        // ================= LAST 5 TRANSACTIONS =================
        List<DashboardTransactionDto> prepaidTransactions =
                superDistributorPrepaidTransactionDetailsRepository.fetchDashboardTransactions(request.getHierarchyId());

        List<DashboardTransactionDto> postpaidTransactions =
                superDistributorPostpaidTransactionDetailsRepository.fetchDashboardTransactions(request.getHierarchyId());

        List<DashboardTransactionDto> transactions =
                Stream.concat(prepaidTransactions.stream(), postpaidTransactions.stream())
                        .sorted(
                                Comparator
                                        .comparing(DashboardTransactionDto::getTransactionDate, Comparator.reverseOrder())
                                        .thenComparing(DashboardTransactionDto::getTransactionTime, Comparator.reverseOrder())
                        )
                        .limit(5)
                        .toList();

        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd-MM-yy");
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("hh:mm a");

        List<Map<String, String>> tableData = transactions.stream()
                .map(t -> Map.of(
                        "dateTime",
                        (t.getTransactionDate() != null && t.getTransactionTime() != null)
                                ? t.getTransactionDate().format(dateFormatter) + " " +
                                t.getTransactionTime().format(timeFormatter)
                                : "",
                        "wallet", Optional.ofNullable(t.getWalletType()).orElse(""),
                        "product", Optional.ofNullable(t.getProductName()).orElse(""),
                        "status", Optional.ofNullable(t.getTranStatus()).orElse(""),
                        "amount", t.getAmount() != null ? t.getAmount().toPlainString() : "0"
                ))
                .toList();

        List<Map<String, Object>> cards = List.of(
                Map.of("label", "TRANSACTION VOLUME",
                        "value", aggregationService.getTransactionVolume(
                                sdId, "SD", fromDate, toDate)),
                Map.of("label", "PENDING TRANSACTION",
                        "value", aggregationService.getStrategy("SD")
                                .getPendingTransactionCount(sdId, fromDate, toDate)),
                Map.of("label", "RETAILERS CREATED",
                        "value", aggregationService.getStrategy("SD")
                                .getRetailerCount(sdId)),
                Map.of("label", "COMMISSION EARNED",
                        "value", aggregationService.getCommission(
                                sdId, "SD", fromDate, toDate)),
                Map.of("label", "FUND RECEIVED",
                        "value", aggregationService.getFundReceived(
                                sdId,"SD", fromDate, toDate)),
                Map.of("label", "FUND TRANSFERRED",
                        "value", aggregationService.getFundTransferred(
                                sdId, "SD", fromDate, toDate))

        );

        return Map.of(
                "profile", profile,
                "cards", cards,
                "tableData", tableData,
                "graphData", aggregationService.getStrategy("SD").getLast4MonthsGraphData(sdId),
                "servicePie", aggregationService.getStrategy("SD").getServicePieData(sd.getSuperDistributorId())
        );
    }
}