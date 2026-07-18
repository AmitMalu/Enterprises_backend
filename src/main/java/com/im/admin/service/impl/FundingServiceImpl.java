package com.im.admin.service.impl;

import com.im.admin.dto.*;
import com.im.admin.entities.*;
import com.im.admin.entities.enums.TransactionType;
import com.im.admin.entities.enums.WalletType;
import com.im.admin.entities.product.Products;
import com.im.admin.repositories.*;
import com.im.admin.request.HierarchyWalletReportRequest;
import com.im.admin.response.BulkFundResponse;
import com.im.admin.response.HierarchyWalletReportDto;
import com.im.admin.service.FundingService;
import com.im.admin.util.CommonMethods;
import com.im.admin.util.TokenUtil;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;


@Service
@Slf4j
public class FundingServiceImpl implements FundingService {

    @Autowired
     private HierarchyWalletReportRepository repository;

    @Autowired
    private AdminRepository adminRepository;
    private SuperDistributorRepository superDistributorRepository;

    @Autowired
    private WhiteLabelRepository whiteLabelRepository;

    @Autowired
    private EnterprisesRepository enterprisesRepository;

    @Autowired
    private UserDetailsRepository userDetailsRepository;

    @Autowired
    private DistributorRepository distributorRepository;

    @Autowired
    private PartnerRepository partnerRepository;

    @Autowired
    private RetailerRepository retailerRepository;

    @Autowired
    private TokenUtil tokenUtil;

    @Autowired
    private AccessHierarchyPathRepository accessHierarchyPathRepository;

    @Autowired
    private CommonMethods commonMethods;

    @Autowired
    private ProductsRepository productsRepository;

    @Autowired
    private RetailerPrepaidTransactionDetailsRepository retailerPrepaidTransactionDetailsRepository;

    @Autowired
    private RetailerPostpaidTransactionDetailsRepository retailerPostpaidTransactionDetailsRepository;

    @Autowired
    private SubProductsRepository subProductsRepository;

    @Autowired
    private VendorRepository vendorRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Override
    @Transactional
    public String fundsCreditDebit(FundCreditDebitDto dto, Double latitude, Double longitude, String ipAddress) {
        log.info("Starting funds {} operation for userAccountId: {}, WalletType: {}, Amount: {}",
                dto.getFundingType(), dto.getUserAccountId(), dto.getWalletType(), dto.getAmount());

        // Generate base transaction ID
        String baseTxnId = commonMethods.generateBaseTransactionId();
        log.debug("Generated base transaction ID: {}", baseTxnId);

        // Extract and resolve types
        String adminId = dto.getAdminId();
        String userId = dto.getUserAccountId();
        WalletType walletType = WalletType.fromString(dto.getWalletType());
        TransactionType transactionType = TransactionType.fromString(dto.getFundingType());
        log.debug("Resolved WalletType: {}, TransactionType: {}", walletType, transactionType);

        BigDecimal amount = BigDecimal.valueOf(dto.getAmount());
        BigDecimal confirmAmount = BigDecimal.valueOf(dto.getConfirmAmount());

        if (amount.compareTo(confirmAmount) != 0) {
            throw new IllegalArgumentException("Amount and Confirm Amount do not match");
        }

        // Fetch wallets
        AbstractWalletAmountTable userWallet = commonMethods.getWalletByType(walletType.name(), userId);
        AbstractWalletAmountTable adminWallet = commonMethods.getWalletByType(walletType.name(), adminId);

        // ===== VALIDATE SUFFICIENT BALANCE =====
        if (transactionType.equals(TransactionType.DEBIT)) {
            if (userWallet.getAvailableBalance().compareTo(amount) < 0) {
                log.error("Insufficient balance in user wallet. Available: {}, Required: {}",
                        userWallet.getAvailableBalance(), amount);
                throw new IllegalArgumentException(
                        String.format("Insufficient balance. Available: %s, Required: %s",
                                userWallet.getAvailableBalance(), amount)
                );
            }
        } else if (transactionType.equals(TransactionType.CREDIT)) {
            // For CREDIT, check if admin has sufficient balance (if admin is the source)
            if (adminWallet.getAvailableBalance().compareTo(amount) < 0) {
                log.error("Insufficient balance in admin wallet. Available: {}, Required: {}",
                        adminWallet.getAvailableBalance(), amount);
                throw new IllegalArgumentException(
                        String.format("Insufficient admin balance. Available: %s, Required: %s",
                                adminWallet.getAvailableBalance(), amount)
                );
            }
        }


        // Generate transaction ID prefix
        String txnIdPrefix = transactionType == TransactionType.CREDIT ? "FCR" : "FDR";
        String userTxnId = txnIdPrefix + baseTxnId;
        String adminTxnId = txnIdPrefix + baseTxnId;

        RetailersExtraColumnsDto retailerExtraColumns =
                RetailersExtraColumnsDto.builder()
                        .medium(dto.getMedium())
                        .clientIp(ipAddress)
                        .requestDomainName(dto.getRequestDomainName())
                        .latitude(latitude)
                        .longitude(longitude)
                        .build();

        if (transactionType == TransactionType.CREDIT) {
            // CREDIT => Debit from Admin, Credit to User
            log.info("Performing CREDIT: Admin {} → User {}", adminId, userId);

            // Debit from admin
            commonMethods.performWalletUpdate(
                    adminWallet,
                    adminId,
                    walletType,
                    amount,
                    TransactionType.DEBIT,
                    adminTxnId,
                    dto.getRemarks(),
                    "Fund Credit (Admin Debit)",
                    dto.getNarrationType(),
                    userId, // inserted into hierarchy_id for AD/WL
                    retailerExtraColumns
            );

            // Credit to user
            commonMethods.performWalletUpdate(
                    userWallet,
                    userId,
                    walletType,
                    amount,
                    TransactionType.CREDIT,
                    userTxnId,
                    dto.getRemarks(),
                    "Fund Credit (User Credit)",
                    dto.getNarrationType(),
                    adminId,
                    retailerExtraColumns
            );

        } else if (transactionType == TransactionType.DEBIT) {
            // DEBIT => Debit from User, Credit to Admin
            log.info("Performing DEBIT: User {} → Admin {}", userId, adminId);

            // Debit from user
            commonMethods.performWalletUpdate(
                    userWallet,
                    userId,
                    walletType,
                    amount,
                    TransactionType.DEBIT,
                    userTxnId,
                    dto.getRemarks(),
                    "Fund Debit (User Debit)",
                    dto.getNarrationType(),
                    adminId,
                    retailerExtraColumns
            );

            // Credit to admin
            commonMethods.performWalletUpdate(
                    adminWallet,
                    adminId,
                    walletType,
                    amount,
                    TransactionType.CREDIT,
                    adminTxnId,
                    dto.getRemarks(),
                    "Fund Debit (Admin Credit)",
                    dto.getNarrationType(),
                    userId, // inserted into hierarchy_id for AD/WL
                    retailerExtraColumns
            );
        }

        log.info("Transaction {} successful for user {} with baseTxnId {}", transactionType.name(), userId, baseTxnId);
        return String.format("Transaction %s successful (BaseTxnId: %s)", transactionType.name(), baseTxnId);
    }


    @Override
    public void downloadSampleFile(HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=bulk-credit-debit-sample.xlsx");

        String[] headers = {
                "fundingType", "userAccountId", "walletType",
                "amount", "confirmAmount", "narrationType", "remarks"
        };

        Object[] sampleValues = {
                "CREDIT or DEBIT", "ABC-RT-001", "PREPAID or POSTPAID",
                1000.00, 1000.00, "FUND_ADDITION", "Bulk upload example"
        };

        try (Workbook workbook = new XSSFWorkbook();
             ServletOutputStream out = response.getOutputStream()) {

            Sheet sheet = workbook.createSheet("Credit-Debit");

            createRow(sheet, 0, headers);

            createRow(sheet, 1, sampleValues);

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
        }
    }

    /**
     * Helper method to create a row with values
     */
    private void createRow(Sheet sheet, int rowIndex, Object[] values) {
        Row row = sheet.createRow(rowIndex);
        for (int i = 0; i < values.length; i++) {
            if (values[i] instanceof Number) {
                row.createCell(i).setCellValue(((Number) values[i]).doubleValue());
            } else {
                row.createCell(i).setCellValue(String.valueOf(values[i]));
            }
        }
    }

    @Override
    public List<BulkFundResponse> processBulkFile(MultipartFile file, Double latitude, Double longitude, String ipAddress) {
        List<BulkFundResponse> responses = new ArrayList<>();

        // create a thread pool based on CPU cores
        int threads = Runtime.getRuntime().availableProcessors();
        ExecutorService executorService = Executors.newFixedThreadPool(threads);

        try (InputStream is = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(is)) {

            Sheet sheet = workbook.getSheetAt(0);
            List<CompletableFuture<BulkFundResponse>> futures = new ArrayList<>();

            // Skip header row
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);

                FundCreditDebitDto dto = new FundCreditDebitDto();
                dto.setFundingType(row.getCell(0).getStringCellValue().trim());
                dto.setUserAccountId(row.getCell(1).getStringCellValue().trim());
                dto.setWalletType(row.getCell(2).getStringCellValue().trim());
                dto.setAmount(row.getCell(3).getNumericCellValue());
                dto.setConfirmAmount(row.getCell(4).getNumericCellValue());
                dto.setNarrationType(row.getCell(5).getStringCellValue());
                dto.setRemarks(row.getCell(6).getStringCellValue());
                dto.setAdminId(row.getCell(7).getStringCellValue());

                int rowNum = i + 1; // keep track of Excel row for error message

                // Submit async task
                CompletableFuture<BulkFundResponse> future =
                        CompletableFuture.supplyAsync(() -> processSingleRow(dto, rowNum, latitude, longitude, ipAddress), executorService)
                                .exceptionally(ex -> {
                                    BulkFundResponse failed = new BulkFundResponse();
                                    failed.setStatus("FAILED");
                                    failed.setMessage("Row " + rowNum + ": " + ex.getMessage());
                                    return failed;
                                });

                futures.add(future);
            }

            responses = futures.stream()
                    .map(CompletableFuture::join)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            throw new RuntimeException("Failed to process Excel file: " + e.getMessage(), e);
        } finally {
            executorService.shutdown();
        }

        return responses;
    }

    /**
     * Process a single Excel row into a transaction safely.
     */
    private BulkFundResponse processSingleRow(FundCreditDebitDto dto, int rowNum, Double latitude, Double longitude, String ipAddress) {
        BulkFundResponse response = new BulkFundResponse();
        response.setUserAccountId(dto.getUserAccountId());
        response.setConfirmAmount(BigDecimal.valueOf(dto.getConfirmAmount()));
        response.setWalletType(dto.getWalletType());
        response.setFundingType(dto.getFundingType());
        response.setAmount(BigDecimal.valueOf(dto.getAmount()));

        try {
            TransactionTemplate template = new TransactionTemplate(transactionManager);
            template.execute(status -> {
                String result = fundsCreditDebit(dto, latitude, longitude, ipAddress); // now runs inside a TX
                response.setStatus("SUCCESS");
                response.setMessage(result);
                return null;
            });
        } catch (Exception e) {
            response.setStatus("FAILED");
            response.setMessage("Row " + rowNum + ": " + e.getMessage());
        }

        return response;
    }


    @Override
    public Page<RetailerTransactionResponseDto> getSettlementPendingTransactions(
            SettlementPendingRequestDto request, int page, int size) {

        // ===== NETWORK HIERARCHY LOGIC =====
        String hierarchyId = tokenUtil.extractHierarchyId();

        List<String> networkIds = getRetailersUnderHierarchy(hierarchyId);
        log.info("Settlement Network IDs: {}", networkIds);

        log.info("Fetching Settlement Transactions for walletType: {}", request.getWalletType());

        List<String> filteredSubProducts = null;

        // =========================================================
        //  NEW: HANDLE subProductName ONLY (IMPORTANT FIX)
        // =========================================================
        if (StringUtils.hasText(request.getSubProductName())
                && !StringUtils.hasText(request.getSubProductId())
                && request.getProductId() == null
                && !StringUtils.hasText(request.getProductName())) {

            List<String> requestedSubs = Arrays.stream(request.getSubProductName().split(","))
                    .map(String::trim)
                    .toList();

            filteredSubProducts = subProductsRepository.findAll().stream()
                    .filter(sp ->
                            requestedSubs.contains(sp.getSubProductTitle()) ||
                                    requestedSubs.contains(sp.getSubProductCode())
                    )
                    .flatMap(sp -> Stream.of(
                            sp.getSubProductTitle(),
                            sp.getSubProductCode()
                    ))
                    .distinct()
                    .toList();
        }

        // =========================================================
        // 1. PRIORITY: subProductId (STRICT FILTER)
        // =========================================================
        else if (StringUtils.hasText(request.getSubProductId())) {

            SubProducts subProduct = subProductsRepository.findById(request.getSubProductId())
                    .orElseThrow(() -> new IllegalArgumentException("Invalid subProductId"));

            filteredSubProducts = List.of(subProduct.getSubProductCode());
        }

        // =========================================================
        // 2. FALLBACK: product + subProductName (OPTIONAL)
        // =========================================================
        else if (request.getProductId() != null) {

            Products product = productsRepository.findById(request.getProductId())
                    .orElseThrow(() -> new IllegalArgumentException("Invalid productId"));

            List<String> allSubProducts = subProductsRepository.findByProduct(product)
                    .stream()
                    .map(SubProducts::getSubProductCode)
                    .toList();

            if (StringUtils.hasText(request.getSubProductName())) {

                List<String> requestedSubs = Arrays.stream(request.getSubProductName().split(","))
                        .map(String::trim)
                        .toList();

                filteredSubProducts = subProductsRepository.findAll().stream()
                        .filter(sp ->
                                requestedSubs.contains(sp.getSubProductTitle()) ||
                                        requestedSubs.contains(sp.getSubProductCode())
                        )
                        .flatMap(sp -> Stream.of(
                                sp.getSubProductTitle(),
                                sp.getSubProductCode()
                        ))
                        .distinct()
                        .toList();
            } else {
                filteredSubProducts = allSubProducts;
            }
        }

        // =========================================================
        // PRODUCT NAME FILTER
        // =========================================================
        else if (StringUtils.hasText(request.getProductName())) {

            filteredSubProducts = subProductsRepository.findAll().stream()
                    .filter(sp -> sp.getProduct() != null
                            && request.getProductName().equalsIgnoreCase(sp.getProduct().getName()))
                    .map(SubProducts::getSubProductCode)
                    .toList();
        }

        log.info("Using subProducts: {}", filteredSubProducts);

        // =========================================================
        // Vendor
        // =========================================================
        String vendorName = null;
        if (StringUtils.hasText(request.getVendorId())) {
            vendorName = vendorRepository.findById(request.getVendorId())
                    .orElseThrow(() -> new IllegalArgumentException("Invalid vendorId"))
                    .getVendorName();
        } else if (StringUtils.hasText(request.getVendor())) {
            vendorName = request.getVendor();
        }

        Pageable pageable = PageRequest.of(page, size,
                Sort.by(
                        Sort.Order.desc("updatedDateOfTransaction"),
                        Sort.Order.desc("updatedTimeOfTransaction")
                )
        );

        // =========================================================
        // Status list
        // =========================================================
        List<String> statusList;
        if ("POST SETTLEMENT".equalsIgnoreCase(request.getTranStatus())) {
            statusList = Arrays.asList("SUCCESS", "FAILURE");
        } else if (!StringUtils.hasText(request.getTranStatus())
                || "ALL".equalsIgnoreCase(request.getTranStatus())) {
            statusList = List.of("PENDING");
            //statusList = null;
        } else {
            statusList = Collections.singletonList(request.getTranStatus());
        }

        // =========================================================
        // FETCH DATA
        // =========================================================
        Page<RetailerTransactionResponseDto> responsePage;

        if ("PREPAID".equalsIgnoreCase(request.getWalletType())) {
            responsePage = fetchPrepaid(request, filteredSubProducts, vendorName, statusList, networkIds, pageable);
        } else if ("POSTPAID".equalsIgnoreCase(request.getWalletType())) {
            responsePage = fetchPostpaid(request, filteredSubProducts, vendorName, statusList, networkIds, pageable);
        } else {

            Page<RetailerTransactionResponseDto> prepaid =
                    fetchPrepaid(request, filteredSubProducts, vendorName, statusList, networkIds, pageable);

            Page<RetailerTransactionResponseDto> postpaid =
                    fetchPostpaid(request, filteredSubProducts, vendorName, statusList, networkIds, pageable);

            List<RetailerTransactionResponseDto> merged =
                    Stream.concat(prepaid.stream(), postpaid.stream())
                            .sorted(Comparator.comparing(
                                    RetailerTransactionResponseDto::getUpdatedAt,
                                    Comparator.nullsLast(Comparator.naturalOrder())
                            ).reversed())
                            .collect(Collectors.toList());

            int start = (int) pageable.getOffset();
            int end = Math.min(start + pageable.getPageSize(), merged.size());

            List<RetailerTransactionResponseDto> paginatedList =
                    start >= merged.size()
                            ? Collections.emptyList()
                            : merged.subList(start, end);

            long totalElements =
                    prepaid.getTotalElements() +
                            postpaid.getTotalElements();

            responsePage = new PageImpl<>(
                    paginatedList,
                    pageable,
                    totalElements
            );
        }

        return responsePage;
    }


    private Page<RetailerTransactionResponseDto> fetchPrepaid(
            SettlementPendingRequestDto request,
            List<String> subProductNames,
            String vendorName,
            List<String> statusList,
            List<String> networkIds,
            Pageable pageable) {

        Map<String, SubProducts> subProductMap = getSubProductMap();

        // ===== LAT LONG RANGE =====
        Double latMin = null, latMax = null, longMin = null, longMax = null;

        if (request.getLatitude() != null) {
            latMin = request.getLatitude() - 0.0001;
            latMax = request.getLatitude() + 0.0001;
        }

        if (request.getLongitude() != null) {
            longMin = request.getLongitude() - 0.0001;
            longMax = request.getLongitude() + 0.0001;
        }

        return retailerPrepaidTransactionDetailsRepository.findFilteredTransactions(
                request.getId(),
                request.getFromDate(),
                request.getToDate(),
                subProductNames,
                vendorName,
                statusList,
                networkIds,
                request.getUserId(),
                request.getMobileNo(),
                request.getSchemeName(),
                request.getAmount(),
                request.getTxId(),
                request.getRemarks(),
                request.getMedium(),
                request.getTxType(),
                request.getTxStatus(),
                request.getDateOfTransaction(),
                request.getTimeOfTransaction(),
                request.getUpdatedDateOfTransaction(),
                request.getUpdatedTimeOfTransaction(),
                latMin,
                latMax,
                longMin,
                longMax,
                pageable
        ).map(txn -> {

            SubProducts sp = subProductMap.get(txn.getSubProductName());

            RetailerTransactionResponseDto dto =
                    new RetailerTransactionResponseDto(
                            txn.getId(),
                            txn.getUpdatedDateOfTransaction(),
                            txn.getRetailerId(),
                            txn.getMobileNo(),
                            txn.getSchemeName(),
                            txn.getAmount(),
                            txn.getTranStatus(),
                            txn.getTransactionType(),
                            txn.getRemarks(),
                            txn.getTransactionId(),
                            txn.getMedium(),
                            txn.getSubProductName(),
                            txn.getVendorName(),
                            txn.getDateOfTransaction(),
                            txn.getTimeOfTransaction(),
                            txn.getUpdatedDateOfTransaction(),
                            txn.getUpdatedTimeOfTransaction(),
                            txn.getVendorTransactionId(),
                            txn.getLatitude(),
                            txn.getLongitude(),
                            txn.getOperatorName()
                    );

            dto.setWalletType("PREPAID");
            dto.setProductName(sp != null ? sp.getProduct().getName() : null);

            return dto;
        });
    }


    private Page<RetailerTransactionResponseDto> fetchPostpaid(
            SettlementPendingRequestDto request,
            List<String> subProductNames,
            String vendorName,
            List<String> statusList,
            List<String> networkIds,
            Pageable pageable) {

        Map<String, SubProducts> subProductMap = getSubProductMap();

        // ===== LAT LONG RANGE =====
        Double latMin = null, latMax = null, longMin = null, longMax = null;

        if (request.getLatitude() != null) {
            latMin = request.getLatitude() - 0.0001;
            latMax = request.getLatitude() + 0.0001;
        }

        if (request.getLongitude() != null) {
            longMin = request.getLongitude() - 0.0001;
            longMax = request.getLongitude() + 0.0001;
        }

        return retailerPostpaidTransactionDetailsRepository.findFilteredTransactions(
                request.getId(),
                request.getFromDate(),
                request.getToDate(),
                subProductNames,
                vendorName,
                statusList,
                networkIds,
                request.getUserId(),
                request.getMobileNo(),
                request.getSchemeName(),
                request.getAmount(),
                request.getTxId(),
                request.getRemarks(),
                request.getMedium(),
                request.getTxType(),
                request.getTxStatus(),
                request.getDateOfTransaction(),
                request.getTimeOfTransaction(),
                request.getUpdatedDateOfTransaction(),
                request.getUpdatedTimeOfTransaction(),
                latMin,
                latMax,
                longMin,
                longMax,
                pageable
        ).map(txn -> {

            SubProducts sp = subProductMap.get(txn.getSubProductName());

            RetailerTransactionResponseDto dto =
                    new RetailerTransactionResponseDto(
                            txn.getId(),
                            txn.getUpdatedDateOfTransaction(),
                            txn.getRetailerId(),
                            txn.getMobileNo(),
                            txn.getSchemeName(),
                            txn.getAmount(),
                            txn.getTranStatus(),
                            txn.getTransactionType(),
                            txn.getRemarks(),
                            txn.getTransactionId(),
                            txn.getMedium(),
                            txn.getSubProductName(),
                            txn.getVendorName(),
                            txn.getDateOfTransaction(),
                            txn.getTimeOfTransaction(),
                            txn.getUpdatedDateOfTransaction(),
                            txn.getUpdatedTimeOfTransaction(),
                            txn.getVendorTransactionId(),
                            txn.getLatitude(),
                            txn.getLongitude(),
                            txn.getOperatorName()
                    );

            dto.setWalletType("POSTPAID");
            dto.setProductName(sp != null ? sp.getProduct().getName() : null);

            return dto;
        });
    }

//    private Map<String, SubProducts> getSubProductMap() {
//        return subProductsRepository.findAll().stream()
//                .collect(Collectors.toMap(
//                        SubProducts::getSubProductCode,
//                        sp -> sp
//                ));
//    }

    private Map<String, SubProducts> getSubProductMap() {
        Map<String, SubProducts> map = new HashMap<>();

        for (SubProducts sp : subProductsRepository.findAll()) {
            map.put(sp.getSubProductCode(), sp);

            if (sp.getSubProductTitle() != null) {
                map.put(sp.getSubProductTitle(), sp);
            }
        }

        return map;
    }


    @Override
    public Page<RetailerTransactionResponseDto> getSettlementPendingTransactionsUpdated(
            SettlementPendingRequestDto request, int page, int size) {

        log.info("=== START: getSettlementPendingTransactionsUpdated ===");
        log.info("Request parameters - WalletType: {}, ProductId: {}, SubProductName: {}, VendorId: {}, Vendor: {}",
                request.getWalletType(), request.getProductId(), request.getSubProductName(),
                request.getVendorId(), request.getVendor());
        log.info("Pagination - Page: {}, Size: {}", page, size);

        List<String> filteredSubProducts = null;

        if (request.getProductId() != null) {
            log.debug("Processing productId: {}", request.getProductId());

            Products product = productsRepository.findById(request.getProductId())
                    .orElseThrow(() -> {
                        log.error("Invalid productId: {} - Product not found", request.getProductId());
                        return new IllegalArgumentException("Invalid productId");
                    });
            log.debug("Found product: {}", product.getName());

            List<String> allSubProducts = subProductsRepository.findByProduct(product)
                    .stream()
                    .map(SubProducts::getSubProductTitle)
                    .collect(Collectors.toList());
            log.debug("Total sub-products found for product: {}", allSubProducts.size());

            if (StringUtils.hasText(request.getSubProductName())) {
                log.debug("Filtering sub-products based on requested names: {}", request.getSubProductName());

                List<String> requestedSubs = Arrays.stream(request.getSubProductName().split(","))
                        .map(String::trim)
                        .toList();
                log.debug("Requested sub-products after splitting: {}", requestedSubs);

                filteredSubProducts = requestedSubs.stream()
                        .filter(allSubProducts::contains)
                        .collect(Collectors.toList());

                log.info("Filtered sub-products count: {} out of {} requested",
                        filteredSubProducts.size(), requestedSubs.size());
                log.debug("Filtered sub-products list: {}", filteredSubProducts);
            } else {
                filteredSubProducts = allSubProducts;
                log.info("No sub-product filter applied. Using all {} sub-products", allSubProducts.size());
            }
        } else {
            log.debug("No productId provided. Skipping sub-product filtering.");
        }

        String vendorName = null;
        if (StringUtils.hasText(request.getVendorId())) {
            log.debug("Processing vendorId: {}", request.getVendorId());

            vendorName = vendorRepository.findById(request.getVendorId())
                    .orElseThrow(() -> {
                        log.error("Invalid vendorId: {} - Vendor not found", request.getVendorId());
                        return new IllegalArgumentException("Invalid vendorId");
                    })
                    .getVendorName();
            log.info("Found vendor by ID - vendorId: {}, vendorName: {}", request.getVendorId(), vendorName);
        } else if (StringUtils.hasText(request.getVendor())) {
            vendorName = request.getVendor();
            log.debug("Using vendor name from request: {}", vendorName);
        } else {
            log.debug("No vendor filter applied");
        }

        Pageable pageable = PageRequest.of(page, size,
                Sort.by("updatedDateOfTransaction").descending());
        log.debug("Created Pageable with sorting: updatedDateOfTransaction DESC");

        Page<RetailerTransactionResponseDto> result;

        if ("PREPAID".equalsIgnoreCase(request.getWalletType())) {
            log.info("Processing PREPAID wallet type");
            result = fetchPrepaidUpdated(request, filteredSubProducts, vendorName, pageable);
            log.info("PREPAID query completed - Total elements: {}, Total pages: {}",
                    result.getTotalElements(), result.getTotalPages());
        } else {
            log.info("Processing POSTPAID wallet type");
            result = fetchPostpaidUpdated(request, filteredSubProducts, vendorName, pageable);
            log.info("POSTPAID query completed - Total elements: {}, Total pages: {}",
                    result.getTotalElements(), result.getTotalPages());
        }

        log.info("=== END: getSettlementPendingTransactionsUpdated - Returning {} records ===",
                result.getContent().size());

        return result;
    }

    private Page<RetailerTransactionResponseDto> fetchPrepaidUpdated(
            SettlementPendingRequestDto request,
            List<String> subProductNames,
            String vendorName,
            Pageable pageable) {

        log.info("Fetching PREPAID transactions with filters - SubProducts: {}, Vendor: {}",
                subProductNames, vendorName);

        Map<String, SubProducts> subProductMap = getSubProductMap();

        Page<RetailerTransactionResponseDto> result = retailerPrepaidTransactionDetailsRepository
                .findFilteredTransactionsUpdated(
                        request.getId(),
                        request.getFromDate(),
                        request.getToDate(),
                        subProductNames,
                        vendorName,
                        null, // statusList REMOVED
                        request.getUserId(),
                        request.getMobileNo(),
                        request.getSchemeName(),
                        request.getAmount(),
                        request.getTxId(),
                        request.getRemarks(),
                        request.getMedium(),
                        request.getTxType(),
                        request.getTxStatus(),
                        request.getActionOnBalanceAmount(), // Settlement Pending filter
                        request.getDateOfTransaction(),
                        request.getTimeOfTransaction(),
                        request.getUpdatedDateOfTransaction(),
                        request.getUpdatedTimeOfTransaction(),
                        pageable
                ).map(txn -> {

                    SubProducts sp = subProductMap.get(txn.getSubProductName());

                    RetailerTransactionResponseDto dto =
                            new RetailerTransactionResponseDto(
                                    txn.getId(),
                                    txn.getUpdatedDateOfTransaction(),
                                    txn.getRetailerId(),
                                    txn.getMobileNo(),
                                    txn.getSchemeName(),
                                    txn.getAmount(),
                                    txn.getTranStatus(),
                                    txn.getTransactionType(),
                                    txn.getRemarks(),
                                    txn.getTransactionId(),
                                    txn.getMedium(),
                                    txn.getSubProductName(),
                                    txn.getVendorName(),
                                    txn.getDateOfTransaction(),
                                    txn.getTimeOfTransaction(),
                                    txn.getUpdatedDateOfTransaction(),
                                    txn.getUpdatedTimeOfTransaction(),
                                    txn.getVendorTransactionId(),
                                    txn.getLatitude(),
                                    txn.getLongitude(),
                                    txn.getOperatorName()
                            );

                    dto.setWalletType("PREPAID");
                    dto.setProductName(sp != null ? sp.getProduct().getName() : null);

                    return dto;
                });

        log.info("PREPAID query returned {} records", result.getContent().size());
        return result;
    }

    private Page<RetailerTransactionResponseDto> fetchPostpaidUpdated(
            SettlementPendingRequestDto request,
            List<String> subProductNames,
            String vendorName,
            Pageable pageable) {

        log.info("Fetching POSTPAID transactions with filters - SubProducts: {}, Vendor: {}",
                subProductNames, vendorName);

        Map<String, SubProducts> subProductMap = getSubProductMap();

        String actionFilter = StringUtils.hasText(request.getActionOnBalanceAmount())
                ? request.getActionOnBalanceAmount()
                : null;

        Page<RetailerTransactionResponseDto> result = retailerPostpaidTransactionDetailsRepository
                .findFilteredTransactionsUpdated(
                        request.getId(),
                        request.getFromDate(),
                        request.getToDate(),
                        subProductNames,
                        vendorName,
                        request.getUserId(),
                        request.getMobileNo(),
                        request.getSchemeName(),
                        request.getAmount(),
                        request.getTxId(),
                        request.getRemarks(),
                        request.getMedium(),
                        request.getTxType(),
                        actionFilter, // ONLY THIS FILTER MATTERS
                        request.getDateOfTransaction(),
                        request.getTimeOfTransaction(),
                        request.getUpdatedDateOfTransaction(),
                        request.getUpdatedTimeOfTransaction(),
                        pageable
                ).map(txn -> {

                    SubProducts sp = subProductMap.get(txn.getSubProductName());

                    RetailerTransactionResponseDto dto =
                            new RetailerTransactionResponseDto(
                                    txn.getId(),
                                    txn.getUpdatedDateOfTransaction(),
                                    txn.getRetailerId(),
                                    txn.getMobileNo(),
                                    txn.getSchemeName(),
                                    txn.getAmount(),
                                    txn.getTranStatus(),
                                    txn.getTransactionType(),
                                    txn.getRemarks(),
                                    txn.getTransactionId(),
                                    txn.getMedium(),
                                    txn.getSubProductName(),
                                    txn.getVendorName(),
                                    txn.getDateOfTransaction(),
                                    txn.getTimeOfTransaction(),
                                    txn.getUpdatedDateOfTransaction(),
                                    txn.getUpdatedTimeOfTransaction(),
                                    txn.getVendorTransactionId(),
                                    txn.getLatitude(),
                                    txn.getLongitude(),
                                    txn.getOperatorName()
                            );

                    dto.setWalletType("POSTPAID");
                    dto.setProductName(sp != null ? sp.getProduct().getName() : null);
                    return dto;
                });

        log.info("POSTPAID query returned {} records", result.getContent().size());
        return result;
    }

    @Override
    public Page<RetailerTransactionResponseDto> getTransactionReport(
            SettlementPendingRequestDto request, int page, int size) {

        log.info("Fetching transactions for walletType: {}", request.getWalletType());

        // network hierarchy logic
        String hierarchyId = tokenUtil.extractHierarchyId();

        List<String> networkIds =
                getRetailersUnderHierarchy(hierarchyId);

        log.info("Network IDs for report: {}", networkIds);

        List<String> filteredSubProducts = null;

        if (request.getProductId() != null) {
            Products product = productsRepository.findById(request.getProductId())
                    .orElseThrow(() -> new IllegalArgumentException("Invalid productId"));

            List<String> allSubProducts = subProductsRepository.findByProduct(product)
                    .stream()
                    .map(SubProducts::getSubProductTitle)
                    .collect(Collectors.toList());

            if (StringUtils.hasText(request.getSubProductName())) {
                List<String> requestedSubs = Arrays.stream(request.getSubProductName().split(","))
                        .map(String::trim)
                        .toList();

                filteredSubProducts = requestedSubs.stream()
                        .filter(allSubProducts::contains)
                        .collect(Collectors.toList());
            } else {
                filteredSubProducts = allSubProducts;
            }
        }

        if (StringUtils.hasText(request.getProductName())) {

            List<SubProducts> subProductsByProductName =
                    subProductsRepository.findAll().stream()
                            .filter(sp -> sp.getProduct() != null &&
                                    sp.getProduct().getName() != null &&
                                    sp.getProduct().getName().toLowerCase()
                                            .contains(request.getProductName().toLowerCase())
                            )
                            .toList();

            List<String> productBasedSubProducts =
                    subProductsByProductName.stream()
                            .map(SubProducts::getSubProductTitle)
                            .collect(Collectors.toList());

            if (filteredSubProducts != null) {
                filteredSubProducts = filteredSubProducts.stream()
                        .filter(productBasedSubProducts::contains)
                        .collect(Collectors.toList());
            } else {
                filteredSubProducts = productBasedSubProducts;
            }
        }

        String vendorName = null;
        if (StringUtils.hasText(request.getVendorId())) {
            vendorName = vendorRepository.findById(request.getVendorId())
                    .orElseThrow(() -> new IllegalArgumentException("Invalid vendorId"))
                    .getVendorName();
        } else if (StringUtils.hasText(request.getVendor())) {
            vendorName = request.getVendor();
        }

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(
                        Sort.Order.desc("dateOfTransaction"),
                        Sort.Order.desc("timeOfTransaction")
                )
        );

        if ("PREPAID".equalsIgnoreCase(request.getWalletType())) {
            return fetchPrepaidTXN(request, filteredSubProducts, vendorName, networkIds, pageable);
        }
        else if ("POSTPAID".equalsIgnoreCase(request.getWalletType())) {
            return fetchPostpaidTXN(request, filteredSubProducts, vendorName, networkIds, pageable);
        }
        else {

//            List<RetailerTransactionResponseDto> combinedList = new ArrayList<>();
//            // walletType null → fetch both
//            Page<RetailerTransactionResponseDto> prepaid =
//                    fetchPrepaidTXN(request, filteredSubProducts, vendorName, networkIds, pageable);
//
//            Page<RetailerTransactionResponseDto> postpaid =
//                    fetchPostpaidTXN(request, filteredSubProducts, vendorName, networkIds, pageable);
//
//            combinedList.add(prepaid);
//            combinedList.add(postpaid);
//
//            List<RetailerTransactionResponseDto> combined =
//                    Stream.concat(prepaid.getContent().stream(),
//                                    postpaid.getContent().stream())
//                            .sorted(Comparator.comparing(
//                                    RetailerTransactionResponseDto::getUpdatedDateOfTransaction,
//                                    Comparator.nullsLast(Comparator.reverseOrder())
//                            ))
//                            .collect(Collectors.toList());
//
//            return new PageImpl<>(
//                    combined,
//                    pageable,
//                    prepaid.getTotalElements() + postpaid.getTotalElements()
//            );


            // ================= BOTH =================

            List<RetailerTransactionResponseDto> combinedList = new ArrayList<>();

            List<RetailerTransactionResponseDto> prepaid =
                    fetchPrepaidTXN(request, filteredSubProducts, vendorName, networkIds, Pageable.unpaged())
                            .getContent();

            List<RetailerTransactionResponseDto> postpaid =
                    fetchPostpaidTXN(request, filteredSubProducts, vendorName, networkIds, Pageable.unpaged())
                            .getContent();

            combinedList.addAll(prepaid);
            combinedList.addAll(postpaid);

            combinedList.sort(
                    Comparator.comparing(
                            RetailerTransactionResponseDto::getDateOfTransaction,
                            Comparator.nullsLast(Comparator.reverseOrder())
                    ).thenComparing(
                            RetailerTransactionResponseDto::getTimeOfTransaction,
                            Comparator.nullsLast(Comparator.reverseOrder())
                    )
            );

            int start = (int) pageable.getOffset();
            int end = Math.min(start + pageable.getPageSize(), combinedList.size());

            List<RetailerTransactionResponseDto> paginatedList =
                    start > end ? Collections.emptyList() : combinedList.subList(start, end);

            return new PageImpl<>(paginatedList, pageable, combinedList.size());
        }

    }


    private Page<RetailerTransactionResponseDto> fetchPrepaidTXN(
            SettlementPendingRequestDto request,
            List<String> subProductNames,
            String vendorName,
            List<String> networkIds,

            Pageable pageable) {

        Map<String, SubProducts> subProductMap = getSubProductMap();

        return retailerPrepaidTransactionDetailsRepository.findFilteredTransactionTXN(
                request.getId(),
                request.getFromDate(),
                request.getToDate(),
                subProductNames,
                vendorName,
                null, //  statusList REMOVED
                networkIds,
                request.getUserId(),
                request.getMobileNo(),
                request.getSchemeName(),
                request.getAmount(),
                request.getTxId(),
                request.getRemarks(),
                request.getMedium(),
                request.getTxType(),

//                request.getTranStatus(),
//                request.getActionOnBalanceAmount(), //  Settlement Pending filter

                request.getActionOnBalanceAmount(), // actionOnBalanceAmount
                request.getTxStatus(),

                request.getDateOfTransaction(),
                request.getTimeOfTransaction(),
                request.getUpdatedDateOfTransaction(),
                request.getUpdatedTimeOfTransaction(),
                pageable
        ).map(txn -> {

            SubProducts sp = subProductMap.get(txn.getSubProductName());

            RetailerTransactionResponseDto dto =
                    new RetailerTransactionResponseDto(
                            txn.getId(),
                            txn.getUpdatedDateOfTransaction(),
                            txn.getRetailerId(),
                            txn.getMobileNo(),
                            txn.getSchemeName(),
                            txn.getAmount(),
                            txn.getTranStatus(),
                            txn.getTransactionType(),
                            txn.getRemarks(),
                            txn.getTransactionId(),
                            txn.getMedium(),
                            txn.getSubProductName(),
                            txn.getVendorName(),
                            txn.getDateOfTransaction(),
                            txn.getTimeOfTransaction(),
                            txn.getUpdatedDateOfTransaction(),
                            txn.getUpdatedTimeOfTransaction(),
                            txn.getVendorTransactionId(),
                            txn.getLatitude(),
                            txn.getLongitude(),
                            txn.getOperatorName()
                    );

            dto.setWalletType("PREPAID");
            dto.setProductName(sp != null ? sp.getProduct().getName() : null);

            return dto;
        });
    }

    private Page<RetailerTransactionResponseDto> fetchPostpaidTXN(
            SettlementPendingRequestDto request,
            List<String> subProductNames,
            String vendorName,
            List<String> networkIds,
            Pageable pageable) {

        Map<String, SubProducts> subProductMap = getSubProductMap();

        String actionFilter = StringUtils.hasText(request.getActionOnBalanceAmount())
                ? request.getActionOnBalanceAmount()
                : null;

        return retailerPostpaidTransactionDetailsRepository.findFilteredTransactionTXN(
                request.getId(),
                request.getFromDate(),
                request.getToDate(),
                subProductNames,
                vendorName,
                networkIds,
                request.getUserId(),
                request.getMobileNo(),
                request.getSchemeName(),
                request.getAmount(),
                request.getTxId(),
                request.getRemarks(),
                request.getMedium(),
                request.getTxType(),
//                request.getTranStatus(),
                request.getTxStatus(),
                actionFilter,   //  ONLY THIS FILTER MATTERS
                request.getDateOfTransaction(),
                request.getTimeOfTransaction(),
                request.getUpdatedDateOfTransaction(),
                request.getUpdatedTimeOfTransaction(),
                pageable
        ).map(txn -> {

            SubProducts sp = subProductMap.get(txn.getSubProductName());

            RetailerTransactionResponseDto dto =
                    new RetailerTransactionResponseDto(
                            txn.getId(),
                            txn.getUpdatedDateOfTransaction(),
                            txn.getRetailerId(),
                            txn.getMobileNo(),
                            txn.getSchemeName(),
                            txn.getAmount(),
                            txn.getTranStatus(),
                            txn.getTransactionType(),
                            txn.getRemarks(),
                            txn.getTransactionId(),
                            txn.getMedium(),
                            txn.getSubProductName(),
                            txn.getVendorName(),
                            txn.getDateOfTransaction(),
                            txn.getTimeOfTransaction(),
                            txn.getUpdatedDateOfTransaction(),
                            txn.getUpdatedTimeOfTransaction(),
                            txn.getVendorTransactionId(),
                            txn.getLatitude(),
                            txn.getLongitude(),
                            txn.getOperatorName()
                    );

            dto.setWalletType("POSTPAID");
            dto.setProductName(sp != null ? sp.getProduct().getName() : null);
            return dto;
        });
    }


//    @Override
//    public List<String> getRetailersUnderHierarchy(
//            String hierarchyId) {
//
//        String hierarchyType =
//                commonMethods.extractHierarchyType(
//                        hierarchyId);
//
//        // Admin sees everything
//        if ("AD".equalsIgnoreCase(hierarchyType)) {
//            return null;
//        }
//
//        // Retailer sees only himself
//        if ("RT".equalsIgnoreCase(hierarchyType)) {
//            return List.of(hierarchyId);
//        }
//
//        return accessHierarchyPathRepository
//                .findRetailerDescendants(
//                        hierarchyId);
//    }

    public List<String> getRetailersUnderHierarchy(String hierarchyId) {

        String type = commonMethods.extractHierarchyType(hierarchyId);

//        if ("RT".equals(type) || "RU".equals(type)) {
//            return List.of(hierarchyId);
//        }
        if ("RU".equals(type)) {
            return List.of(hierarchyId);
        }

        List<String> resultIds = new ArrayList<>();

        // Add itself
        resultIds.add(hierarchyId);

        List<UserDetails> allChildren = new ArrayList<>();

        fetchChildrenRecursively(hierarchyId, allChildren);

        log.info("All children IDs fetched: {}",
                allChildren.stream()
                        .map(UserDetails::getHierarchyId)
                        .toList());


        resultIds.addAll(
                allChildren.stream()
                        .filter(user ->
                                "RT".equalsIgnoreCase(user.getHierarchyCode()) ||
                                        "RU".equalsIgnoreCase(user.getHierarchyCode())
                        )
                        .map(UserDetails::getHierarchyId)
                        .toList()
        );

        return resultIds;
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
    public List<Map<String, String>> getNetworksWithName(String hierarchyId) {

        List<String> ids = getRetailersUnderHierarchy(hierarchyId);

        return ids.stream().map(id -> {
            String type = commonMethods.extractHierarchyType(id);
            String name = resolveNameByType(type, id);
            Map<String, String> entry = new java.util.LinkedHashMap<>();
            entry.put("id",   id);
            entry.put("name", name != null ? name : "");
            return entry;
        }).toList();
    }

    private String resolveNameByType(String type, String id) {
        try {
            return switch (type.toUpperCase()) {
                case "RT" -> retailerRepository.findNameByHierarchyId(id);
                case "DT" -> distributorRepository.findNameByHierarchyId(id);
                case "PT" -> partnerRepository.findNameByHierarchyId(id);
                case "SD" -> superDistributorRepository.findNameByHierarchyId(id);
                case "WL" -> whiteLabelRepository.findNameByHierarchyId(id);
                case "ET" -> enterprisesRepository.findNameByHierarchyId(id);
                case "AD" -> adminRepository.findNameByHierarchyId(id);
                default   -> null;
            };
        } catch (Exception e) {
            log.warn("Could not resolve name for id={} type={}: {}", id, type, e.getMessage());
            return null;
        }
    }




    @Override
    public Page<RetailerTransactionResponseDtoCom> getTransactionReportComWise(
            SettlementPendingRequestDto request, int page, int size) {

        log.info("Fetching transactions for walletType: {}", request.getWalletType());

        String hierarchyId = tokenUtil.extractHierarchyId();
        List<String> networkIds = getRetailersUnderHierarchy(hierarchyId);

        List<String> filteredSubProducts = null;

        if (request.getProductId() != null) {
            Products product = productsRepository.findById(request.getProductId())
                    .orElseThrow(() -> new IllegalArgumentException("Invalid productId"));

            List<String> allSubProducts = subProductsRepository.findByProduct(product)
                    .stream()
                    .map(SubProducts::getSubProductTitle)
                    .toList();

            if (StringUtils.hasText(request.getSubProductName())) {
                List<String> requestedSubs = Arrays.stream(request.getSubProductName().split(","))
                        .map(String::trim)
                        .toList();

                filteredSubProducts = requestedSubs.stream()
                        .filter(allSubProducts::contains)
                        .toList();
            } else {
                filteredSubProducts = allSubProducts;
            }
        }

        String vendorName = null;
        if (StringUtils.hasText(request.getVendorId())) {
            vendorName = vendorRepository.findById(request.getVendorId())
                    .orElseThrow(() -> new IllegalArgumentException("Invalid vendorId"))
                    .getVendorName();
        } else if (StringUtils.hasText(request.getVendor())) {
            vendorName = request.getVendor();
        }

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(
                        Sort.Order.desc("dateOfTransaction"),
                        Sort.Order.desc("timeOfTransaction")
                )
        );

        // ================= WALLET TYPE =================

        if ("PREPAID".equalsIgnoreCase(request.getWalletType())) {
            return fetchPrepaidTXNCom(request, filteredSubProducts, vendorName, networkIds, pageable);
        }

        if ("POSTPAID".equalsIgnoreCase(request.getWalletType())) {
            return fetchPostpaidTXNCom(request, filteredSubProducts, vendorName, networkIds, pageable);
        }

        // ================= BOTH =================

        List<RetailerTransactionResponseDtoCom> combinedList = new ArrayList<>();

        List<RetailerTransactionResponseDtoCom> prepaid =
                fetchPrepaidTXNCom(request, filteredSubProducts, vendorName, networkIds, Pageable.unpaged())
                        .getContent();

        List<RetailerTransactionResponseDtoCom> postpaid =
                fetchPostpaidTXNCom(request, filteredSubProducts, vendorName, networkIds, Pageable.unpaged())
                        .getContent();

        combinedList.addAll(prepaid);
        combinedList.addAll(postpaid);

        // Commission/GST/TDS/Charge filter
        combinedList = combinedList.stream()
                .filter(dto -> request.getCommissionAmount() == null
                        || Objects.equals(dto.getCommissionAmount(), request.getCommissionAmount()))
                .filter(dto -> request.getGstAmount() == null
                        || Objects.equals(dto.getGstAmount(), request.getGstAmount()))
                .filter(dto -> request.getTdsAmount() == null
                        || Objects.equals(dto.getTdsAmount(), request.getTdsAmount()))
                .filter(dto -> request.getChargeAmount() == null
                        || Objects.equals(dto.getChargeAmount(), request.getChargeAmount()))
                .collect(Collectors.toList());

        combinedList.sort(
                Comparator.comparing(
                        RetailerTransactionResponseDtoCom::getDateOfTransaction,
                        Comparator.nullsLast(Comparator.reverseOrder())
                ).thenComparing(
                        RetailerTransactionResponseDtoCom::getTimeOfTransaction,
                        Comparator.nullsLast(Comparator.reverseOrder())
                )
        );

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), combinedList.size());

        List<RetailerTransactionResponseDtoCom> paginatedList =
                start > end ? Collections.emptyList() : combinedList.subList(start, end);

        return new PageImpl<>(paginatedList, pageable, combinedList.size());
    }


    private Page<RetailerTransactionResponseDtoCom> fetchPrepaidTXNCom(
            SettlementPendingRequestDto request,
            List<String> subProductNames,
            String vendorName,
            List<String> networkIds,
            Pageable pageable) {

        Map<String, SubProducts> subProductMap = getSubProductMap();

        return retailerPrepaidTransactionDetailsRepository.findFilteredTransactionTXN(
                request.getId(),
                request.getFromDate(),
                request.getToDate(),
                subProductNames,
                vendorName,
                null,
                networkIds,
                request.getUserId(),
                request.getMobileNo(),
                request.getSchemeName(),
                request.getAmount(),
                request.getTxId(),
                request.getRemarks(),
                request.getMedium(),
//                request.getTxType(),
//                request.getTxStatus(),
//                request.getActionOnBalanceAmount(),
                request.getTxType(),
                request.getActionOnBalanceAmount(),
                request.getTxStatus(),
                request.getDateOfTransaction(),
                request.getTimeOfTransaction(),
                request.getUpdatedDateOfTransaction(),
                request.getUpdatedTimeOfTransaction(),
                pageable
        ).map(txn -> {

            SubProducts sp = subProductMap.get(txn.getSubProductName());

            RetailerTransactionResponseDtoCom dto =
                    new RetailerTransactionResponseDtoCom(
                            txn.getId(),
                            txn.getUpdatedDateOfTransaction(),
                            txn.getRetailerId(),
                            txn.getMobileNo(),
                            txn.getSchemeName(),
                            txn.getAmount(),
                            txn.getTranStatus(),
                            txn.getTransactionType(),
                            txn.getRemarks(),
                            txn.getTransactionId(),
                            txn.getMedium(),
                            txn.getSubProductName(),
                            txn.getVendorName(),
                            txn.getDateOfTransaction(),
                            txn.getTimeOfTransaction(),
                            txn.getUpdatedDateOfTransaction(),
                            txn.getUpdatedTimeOfTransaction()
                    );

            dto.setWalletType("PREPAID");
            dto.setProductName(sp != null ? sp.getProduct().getName() : null);

            // ===============================
            // COMM / GST / TDS / CHG LOGIC
            // ===============================

            String txnId = txn.getTransactionId();

            if (txnId != null && txnId.startsWith("TXN")) {

                String baseId = txnId.substring(3);

                retailerPrepaidTransactionDetailsRepository
                        .findByTransactionId("COM" + baseId)
                        .ifPresent(com -> dto.setCommissionAmount(
                                com.getAmount() != null ? com.getAmount().doubleValue() : 0.0));

                retailerPrepaidTransactionDetailsRepository
                        .findByTransactionId("GST" + baseId)
                        .ifPresent(gst -> dto.setGstAmount(
                                gst.getAmount() != null ? gst.getAmount().doubleValue() : 0.0));

                retailerPrepaidTransactionDetailsRepository
                        .findByTransactionId("TDS" + baseId)
                        .ifPresent(tds -> dto.setTdsAmount(
                                tds.getAmount() != null ? tds.getAmount().doubleValue() : 0.0));

                retailerPrepaidTransactionDetailsRepository
                        .findByTransactionId("CHG" + baseId)
                        .ifPresent(chg -> dto.setChargeAmount(
                                chg.getAmount() != null ? chg.getAmount().doubleValue() : 0.0));
            }

            return dto;
        });
    }

    private Page<RetailerTransactionResponseDtoCom> fetchPostpaidTXNCom(
            SettlementPendingRequestDto request,
            List<String> subProductNames,
            String vendorName,
            List<String> networkIds,
            Pageable pageable) {

        Map<String, SubProducts> subProductMap = getSubProductMap();

        String actionFilter = StringUtils.hasText(request.getActionOnBalanceAmount())
                ? request.getActionOnBalanceAmount()
                : null;

        return retailerPostpaidTransactionDetailsRepository.findFilteredTransactionTXN(
                request.getId(),
                request.getFromDate(),
                request.getToDate(),
                subProductNames,
                vendorName,
                networkIds,
                request.getUserId(),
                request.getMobileNo(),
                request.getSchemeName(),
                request.getAmount(),
                request.getTxId(),
                request.getRemarks(),
                request.getMedium(),
                request.getTxType(),
//                request.getTranStatus(),
                request.getTxStatus(),
                actionFilter,
                request.getDateOfTransaction(),
                request.getTimeOfTransaction(),
                request.getUpdatedDateOfTransaction(),
                request.getUpdatedTimeOfTransaction(),
                pageable
        ).map(txn -> {

            SubProducts sp = subProductMap.get(txn.getSubProductName());

            RetailerTransactionResponseDtoCom dto =
                    new RetailerTransactionResponseDtoCom(
                            txn.getId(),
                            txn.getUpdatedDateOfTransaction(),
                            txn.getRetailerId(),
                            txn.getMobileNo(),
                            txn.getSchemeName(),
                            txn.getAmount(),
                            txn.getTranStatus(),
                            txn.getTransactionType(),
                            txn.getRemarks(),
                            txn.getTransactionId(),
                            txn.getMedium(),
                            txn.getSubProductName(),
                            txn.getVendorName(),
                            txn.getDateOfTransaction(),
                            txn.getTimeOfTransaction(),
                            txn.getUpdatedDateOfTransaction(),
                            txn.getUpdatedTimeOfTransaction()
                    );

            dto.setWalletType("POSTPAID");
            dto.setProductName(sp != null ? sp.getProduct().getName() : null);

            // ===============================
            // COMM / GST / TDS / CHG LOGIC
            // ===============================

            String txnId = txn.getTransactionId();

            if (txnId != null && txnId.startsWith("TXN")) {

                String baseId = txnId.substring(3);

                retailerPostpaidTransactionDetailsRepository
                        .findByTransactionId("COM" + baseId)
                        .ifPresent(com -> dto.setCommissionAmount(
                                com.getAmount() != null ? com.getAmount().doubleValue() : 0.0));

                retailerPostpaidTransactionDetailsRepository
                        .findByTransactionId("GST" + baseId)
                        .ifPresent(gst -> dto.setGstAmount(
                                gst.getAmount() != null ? gst.getAmount().doubleValue() : 0.0));

                retailerPostpaidTransactionDetailsRepository
                        .findByTransactionId("TDS" + baseId)
                        .ifPresent(tds -> dto.setTdsAmount(
                                tds.getAmount() != null ? tds.getAmount().doubleValue() : 0.0));

                retailerPostpaidTransactionDetailsRepository
                        .findByTransactionId("CHG" + baseId)
                        .ifPresent(chg -> dto.setChargeAmount(
                                chg.getAmount() != null ? chg.getAmount().doubleValue() : 0.0));
            }

            return dto;
        });
    }


    @Override
    public Page<HierarchyWalletReportDto> getReport(HierarchyWalletReportRequest request) {

        // Same logic as MyTransactionReport — resolve network CSV from access_hierarchy_path
        String loggedInHierarchyId = tokenUtil.extractHierarchyId();
        String hierarchyIdsCsv     = getWalletPreviewHierarchyIdsCsv(loggedInHierarchyId);

        log.info("getReport (wallet-preview): loggedIn={} hierarchyIdsCsv={}",
                loggedInHierarchyId, hierarchyIdsCsv);

        return repository.getReport(request, hierarchyIdsCsv);
    }

    private String getWalletPreviewHierarchyIdsCsv(String hierarchyId) {
        List<String> hierarchyIds = accessHierarchyPathRepository
                .findDescendantsByAncestor(hierarchyId);

        if (hierarchyIds == null || hierarchyIds.isEmpty()) {
            return hierarchyId;
        }

        return String.join(",", hierarchyIds);
    }

}
