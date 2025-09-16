package com.im.admin.service.impl.reports;


import com.im.admin.dto.WalletReportDto;
import com.im.admin.entities.HourlyReportEntity;
import com.im.admin.entities.enums.ReportType;
import com.im.admin.properties.ReportProperties;
import com.im.admin.repositories.HourlyReportRepository;
import com.im.admin.response.SearchTransactionReportResponseDto;
import com.im.admin.service.reports.AsyncSearchService;
import com.im.admin.service.reports.HourlyReportService;
import com.im.admin.util.ExcelReportGenerator;
import com.im.admin.util.ExcelWalletReportGenerator;
import com.im.admin.util.TransactionPrefixUtil;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HourlyReportServiceImpl implements HourlyReportService {

    @Autowired
    private ReportProperties props;

    @Autowired
    private HourlyReportRepository hourlyReportRepository;

    @Autowired
    private AsyncSearchService asyncSearchService; // fetches transactions from all hierarchies

    private static final DateTimeFormatter FNAME_FMT = DateTimeFormatter.ofPattern("yyyyMMdd_HH");

    @Override
    public Page<HourlyReportEntity> listReportsByDate(LocalDate date, Pageable pageable) {
        return hourlyReportRepository.findByDatedOrderByTimestampAsc(date, pageable);
    }


    @Override
    @Transactional
    public void generateReportsForDate(LocalDate date) throws Exception {
        LocalDateTime startOfDay = date.atStartOfDay();
        for (int h = 0; h < 24; h++) {
            generateReportForHour(startOfDay.plusHours(h));
        }
    }

    @Override
    @Transactional
    public void generateReportForHour(LocalDateTime hourStart) throws Exception {
        LocalDateTime start = hourStart;
        LocalDateTime end = hourStart.plusHours(1);

        CompletableFuture<List<SearchTransactionReportResponseDto>> fut =
                asyncSearchService.searchTransactionsByHour(start, end);
        List<SearchTransactionReportResponseDto> all = fut.get();

        List<SearchTransactionReportResponseDto> txns = all.stream()
                .filter(t -> TransactionPrefixUtil.isTransaction(t.getTransactionId()))
                .toList();

        List<SearchTransactionReportResponseDto> walletsRaw = all.stream()
                .filter(t -> TransactionPrefixUtil.isWallet(t.getTransactionId()))
                .toList();

        // create date folder
        String base = props.getBaseDir();
        String dateFolder = start.toLocalDate().toString();
        File folder = new File(base, dateFolder);
        if (!folder.exists()) folder.mkdirs();

        // --- transaction file ---
        if (!txns.isEmpty()) {
            String fname = "hourly_transaction_" + FNAME_FMT.format(start) + ".xlsx";
            File file = new File(folder, fname);
            ExcelReportGenerator.generateTransactionExcel(txns, file);

            HourlyReportEntity e = new HourlyReportEntity();
            e.setId(start.toLocalDate() + "_" + String.format("%02d", start.getHour()) + "_transaction");
            e.setDated(start.toLocalDate());
            e.setTimestamp(start);
            e.setSubject("Hourly transaction history report " + start);
            e.setFileName(fname);
            e.setFilePath(dateFolder + "/" + fname);
            e.setReportType(ReportType.TRANSACTION);
            hourlyReportRepository.save(e);
        }

        // --- wallet file ---
        if (!walletsRaw.isEmpty()) {
            List<WalletReportDto> walletDtos = walletsRaw.stream().map(t -> {
                WalletReportDto w = new WalletReportDto();
                w.setTransactionId(t.getTransactionId());
                w.setHierarchyId(t.getHierarchyId());
                w.setWalletType(t.getWalletType());
                w.setBalBefore(Optional.ofNullable(t.getBalBefore()).orElse(BigDecimal.ZERO));
                w.setBalAfter(Optional.ofNullable(t.getBalAfter()).orElse(BigDecimal.ZERO));

                BigDecimal change = (t.getBalBefore() != null && t.getBalAfter() != null)
                        ? t.getBalAfter().subtract(t.getBalBefore())
                        : Optional.ofNullable(t.getAmount()).orElse(BigDecimal.ZERO);
                w.setAmount(change);

                w.setDate(t.getDate());
                w.setTime(t.getTime());
                w.setNarration(t.getNarration());
                w.setStatus(t.getStatus());
                w.setSubProductName(t.getSubProductName());
                return w;
            }).toList();

            String fname = "hourly_wallet_" + FNAME_FMT.format(start) + ".xlsx";
            File file = new File(folder, fname);
            ExcelWalletReportGenerator.generateWalletExcel(walletDtos, file);

            HourlyReportEntity e = new HourlyReportEntity();
            e.setId(start.toLocalDate() + "_" + String.format("%02d", start.getHour()) + "_wallet");
            e.setDated(start.toLocalDate());
            e.setTimestamp(start);
            e.setSubject("Hourly wallet history report " + start);
            e.setFileName(fname);
            e.setFilePath(dateFolder + "/" + fname);
            e.setReportType(ReportType.WALLET);
            hourlyReportRepository.save(e);
        }
    }

    @Override
    public void streamReportDownload(String id, HttpServletResponse response) throws Exception {
        HourlyReportEntity e = hourlyReportRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Report not found: " + id));
        File file = new File(props.getBaseDir(), e.getFilePath());
        if (!file.exists()) throw new RuntimeException("File not found: " + file.getAbsolutePath());

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + e.getFileName() + "\"");
        Files.copy(file.toPath(), response.getOutputStream());
        response.flushBuffer();
    }
}
