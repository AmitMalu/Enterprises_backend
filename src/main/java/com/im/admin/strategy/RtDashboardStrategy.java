package com.im.admin.strategy;

import com.im.admin.entities.Retailer;
import com.im.admin.entities.RetailerPostpaidAmount;
import com.im.admin.entities.RetailerPrepaidAmount;
import com.im.admin.repositories.*;
import com.im.admin.request.DashboardTransactionRequest;
import com.im.admin.response.DashboardTransactionDto;
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

@Component("RT")
@RequiredArgsConstructor
public class RtDashboardStrategy implements DashboardStrategy {

    private final RetailerRepository retailerRepository;
    private final RetailerPrepaidTransactionDetailsRepository prepaidRepo;
    private final RetailerPostpaidTransactionDetailsRepository postpaidRepo;
    private final RetailerPrepaidAmountRepository prepaidAmountRepo;
    private final RetailerPostpaidAmountRepository postpaidAmountRepo;

    private final HierarchyAggregationService aggregationService;

    @Override
    public Map<String, Object> buildDashboard(
            DashboardTransactionRequest request,
            LocalDate fromDate,
            LocalDate toDate
    ) {

        String retailerId = request.getHierarchyId();

        Retailer retailer = retailerRepository
                .findByRetailerId(retailerId)
                .orElseThrow(() ->
                        new RuntimeException("Retailer not found"));

        // ================= PROFILE =================
        BigDecimal prepaidBalance =
                prepaidAmountRepo
                        .findByRetailerId_RetailerId(retailerId)
                        .map(RetailerPrepaidAmount::getAvailableBalance)
                        .orElse(BigDecimal.ZERO);

        BigDecimal postpaidBalance =
                postpaidAmountRepo
                        .findByRetailerId_RetailerId(retailerId)
                        .map(RetailerPostpaidAmount::getAvailableBalance)
                        .orElse(BigDecimal.ZERO);

        Map<String, Object> profile = Map.of(
                "image", Optional.ofNullable(retailer.getShopImage()).orElse(""),
                "name", retailer.getOutletAddress().getOutletName(),
                "retailerId", retailerId,
                "prepaid", prepaidBalance,
                "postpaid", postpaidBalance
        );

        // ================= LAST 5 TRANSACTIONS =================
        List<DashboardTransactionDto> prepaid =
                prepaidRepo.fetchDashboardTransactions(retailerId);

        List<DashboardTransactionDto> postpaid =
                postpaidRepo.fetchDashboardTransactions(retailerId);

        List<DashboardTransactionDto> transactions =
                Stream.concat(prepaid.stream(), postpaid.stream())
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

        // ================= AGGREGATION =================
        HierarchyAggregationStrategy strategy =
                aggregationService.getStrategy("RT");

        BigDecimal transactionVolume =
                strategy.getTransactionVolume(retailerId, fromDate, toDate);

        BigDecimal commissionEarned =
                strategy.getCommission(retailerId, fromDate, toDate);

        Long pendingTransactions =
                strategy.getPendingTransactionCount(retailerId, fromDate, toDate);

        BigDecimal fundReceived =
                strategy.getFundReceived(retailerId, fromDate, toDate);

        BigDecimal fundTransferred =
                strategy.getFundTransferred(retailerId, fromDate, toDate);

        List<Map<String, Object>> graphData =
                strategy.getLast4MonthsGraphData(retailerId);

        List<Map<String, Object>> servicePieData =
                strategy.getServicePieData(retailerId);

        // ================= CARDS =================
        List<Map<String, String>> cards = List.of(
                Map.of("label", "TRANSACTION VOLUME",
                        "value", transactionVolume.toPlainString()),
                Map.of("label", "COMMISSION EARNED",
                        "value", commissionEarned.toPlainString()),
                Map.of("label", "PENDING TRANSACTION",
                        "value", pendingTransactions.toString()),
                Map.of("label", "FUND RECEIVED",
                        "value", fundReceived.toPlainString()),
                Map.of("label", "FUND TRANSFERRED",
                        "value", fundTransferred.toPlainString())
        );

        // ================= FINAL RETURN =================
        return Map.of(
                "profile", profile,
                "cards", cards,
                "tableData", tableData,
                "graphData", graphData,
                "servicePieData", servicePieData
        );
    }
}
