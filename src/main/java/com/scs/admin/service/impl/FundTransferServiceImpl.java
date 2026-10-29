package com.scs.admin.service.impl;


import com.scs.admin.dto.*;
import com.scs.admin.entities.FundTransfer;
import com.scs.admin.entities.Scheme;
import com.scs.admin.entities.SchemeRate;
import com.scs.admin.entities.SubProducts;
import com.scs.admin.entities.enums.ServiceType;
import com.scs.admin.entities.enums.Status;
import com.scs.admin.entities.enums.TransactionType;
import com.scs.admin.entities.enums.WalletType;
import com.scs.admin.entities.product.Products;
import com.scs.admin.exception.ResourceNotFoundException;
import com.scs.admin.repositories.FundTransferRepository;
import com.scs.admin.repositories.SchemeRepository;
import com.scs.admin.repositories.SubProductsRepository;
import com.scs.admin.request.FundTransferRequestDto;
import com.scs.admin.response.FundTransferResponseDto;
import com.scs.admin.service.FundTransferService;
import com.scs.admin.util.CommonMethods;
import com.scs.admin.util.WalletTransactionService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;

@Service
@Slf4j
public class FundTransferServiceImpl implements FundTransferService {

    @Autowired
    private FundTransferRepository fundTransferRepository;

    @Autowired
    private CommonMethods commonMethods;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private SubProductsServiceImpl subProductsServiceImpl;

    @Autowired
    private SubProductsRepository subProductsRepository;

    @Autowired
    private TransactionEngineServiceImpl transactionEngineServiceImpl;

    @Autowired
    private WalletTransactionService walletTransactionService;

    @Autowired
    private WalletToWalletTransferServiceImpl walletToWalletTransferServiceImpl;

    @Autowired
    private SchemeRepository schemeRepository;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FundTransferResponseDto transferFunds(FundTransferRequestDto requestDto,
                                                 RequestHeaderMetaDto requestHeaderMetaDtoMeta) {

        log.info("========== Fund Transfer Started ==========");
        log.info("Fund Transfer Request | FromHierarchy={} | ToHierarchy={} | Amount={} | FromWallet={} | ToWallet={} | SubProductId={} | SchemeCode={}",
                requestDto.getFromHierarchyId(),
                requestDto.getToHierarchyId(),
                requestDto.getAmount(),
                requestDto.getFromWalletType(),
                requestDto.getToWalletType(),
                requestDto.getSubProductId(),
                requestDto.getSchemeCode());

        // VALIDATE SUB PRODUCT VISIBILITY
        if (!subProductsServiceImpl.isSubProductVisible(requestDto.getSubProductId())) {
            log.error("SubProduct is not visible. subProductId={}", requestDto.getSubProductId());
            throw new IllegalArgumentException("Selected sub product is currently not visible.");
        }

        log.info("SubProduct is visible and available for wallet transfer. subProductId={}",
                requestDto.getSubProductId());

        validateRequest(requestDto);
        log.info("Fund transfer request validation completed successfully.");

        BigDecimal amount = requestDto.getAmount();
        String fromId = requestDto.getFromHierarchyId();
        String toId = requestDto.getToHierarchyId();
        WalletType fromWalletType = requestDto.getFromWalletType();
        WalletType toWalletType = requestDto.getToWalletType();

        String baseTxnId = commonMethods.generateBaseTransactionId();
        LocalDate date = LocalDate.now();
        LocalTime time = LocalTime.now();

        log.info("Generated Transaction ID: {}", baseTxnId);

        log.info("Fetching sender wallet | HierarchyId={} | WalletType={}",
                fromId, fromWalletType);

        AbstractWalletAmountTable fromWallet =
                commonMethods.getWalletByType(fromWalletType.name(), fromId);

        BigDecimal availableBalance = fromWallet.getAvailableBalance();

        log.info("Sender wallet fetched successfully | AvailableBalance={} | RequestedAmount={}",
                availableBalance, amount);

        if (availableBalance.compareTo(amount) < 0) {
            log.error("Insufficient balance | Available={} | Required={}",
                    availableBalance, amount);
            throw new IllegalStateException("Insufficient wallet balance for transfer.");
        }

        RetailersExtraColumnsDto retailerExtraColumns =
                RetailersExtraColumnsDto.builder()
                        .medium(requestHeaderMetaDtoMeta.getMedium())
                        .clientIp(requestHeaderMetaDtoMeta.getIpAddress())
                        .requestDomainName(requestHeaderMetaDtoMeta.getRequestDomainName())
                        .latitude(requestHeaderMetaDtoMeta.getLatitude())
                        .longitude(requestHeaderMetaDtoMeta.getLongitude())
                        .build();

        log.info("Starting debit transaction | HierarchyId={} | Amount={} | TransactionId={}",
                fromId, amount, baseTxnId);

        // FROM WALLET (DEBIT)
        performWalletUpdate(
                fromWallet,
                fromId,
                fromWalletType,
                amount,
                TransactionType.DEBIT,
                baseTxnId,
                TransactionType.DEBIT.name(),
                "Fund Transfer Successfully to " + requestDto.getToHierarchyId() + "/ @" + date + " " + time,
                date,
                time,
                requestDto.getSubProductId(),
                retailerExtraColumns);

        log.info("Debit completed successfully for HierarchyId={}", fromId);

        log.info("Fetching receiver wallet | HierarchyId={} | WalletType={}",
                toId, toWalletType);

        AbstractWalletAmountTable toWallet =
                commonMethods.getWalletByType(toWalletType.name(), toId);

        log.info("Starting credit transaction | HierarchyId={} | Amount={} | TransactionId={}",
                toId, amount, baseTxnId);

        // TO WALLET (CREDIT)
        performWalletUpdate(
                toWallet,
                toId,
                toWalletType,
                amount,
                TransactionType.CREDIT,
                baseTxnId,
                TransactionType.CREDIT.name(),
                "Received from " + fromId,
                date,
                time,
                requestDto.getSubProductId(),
                retailerExtraColumns);

        log.info("Credit completed successfully for HierarchyId={}", toId);

        // =========================================================
        // RESOLVE PRODUCT, SUB-PRODUCT AND SCHEME
        // =========================================================

        log.info("Fetching SubProduct details for SubProductId={}",
                requestDto.getSubProductId());

        SubProducts subProduct =
                subProductsRepository
                        .findById(requestDto.getSubProductId())
                        .orElseThrow(() -> {

                            log.error("SubProduct NOT FOUND for subProductId={}",
                                    requestDto.getSubProductId());

                            return new RuntimeException(
                                    "SubProduct not found for subProductId: "
                                            + requestDto.getSubProductId());
                        });

        log.info("SubProduct fetched successfully | Id={} | Name={}",
                subProduct.getId(),
                subProduct.getSubProductTitle());

        Products product = subProduct.getProduct();

        log.info("Product resolved successfully | ProductId={} | ProductName={}",
                                                product.getId(), product.getName());

        log.info("Fetching Scheme for SchemeCode={}",
                requestDto.getSchemeCode());

        Scheme scheme =
                schemeRepository.findBySchemeCode(requestDto.getSchemeCode())
                        .orElseThrow(() -> new RuntimeException(
                                "Scheme not found for scheme code: " + requestDto.getSchemeCode()));

        String schemeCode = scheme.getSchemeCode();

        log.info("Scheme resolved successfully | SchemeCode={} | SchemeName={}",
                scheme.getSchemeCode(),
                scheme.getSchemeName());

        log.info("Fetching Scheme Rate | SchemeCode={} | Amount={}",
                schemeCode, amount);

        SchemeRate schemeRate =
                walletToWalletTransferServiceImpl.getExactSchemeRate(
                        schemeCode,
                        amount.doubleValue());

        log.info("Scheme Rate resolved successfully | Gross={} | Operator={}",
                schemeRate.getGross(),
                schemeRate.getOperatorName());

        TransactionDetailsInfoDto transactionDetailsInfo =
                TransactionDetailsInfoDto.builder()
                        .subProductCode(subProduct.getSubProductCode())
                        .schemeName(scheme.getSchemeName())
                        .operatorName(schemeRate.getOperatorName())
                        .vendorName("Internal Product")
                        .build();

        commonMethods.validateDebitTransaction(
                fromId,
                fromWalletType,
                availableBalance,
                amount,
                BigDecimal.valueOf(schemeRate.getGross())
        );

        // =====================================================
        // CHARGES AND COMMISSION
        // =====================================================

        try {

            log.info("Checking Charges configuration | hasCharges={}",
                    product.getHasCharges());

            if (Boolean.TRUE.equals(product.getHasCharges())) {

                log.info("Processing hierarchy charges | HierarchyId={} | TransactionId={}",
                        toId,
                        baseTxnId);

                walletTransactionService.processHierarchyCharges(
                        fromId,
                        product,
                        subProduct,
                        schemeRate,
                        BigDecimal.valueOf(schemeRate.getGross()),
                        baseTxnId,
                        transactionDetailsInfo
                );

                log.info("Hierarchy charges processed successfully.");
            }

            log.info("Checking Commission configuration | isCommissions={}",
                    product.getIsCommissions());

            if (Boolean.TRUE.equals(product.getIsCommissions())) {

                WalletType commissionWalletType =
                        "POSTPAID".equalsIgnoreCase(subProduct.getWalletType())
                                ? WalletType.POSTPAID
                                : WalletType.PREPAID;

                log.info("Building commission transaction DTO.");

                TransactionSchemeDetailsRequestDto dto =
                        walletTransactionService.buildTransactionDto(
                                BigDecimal.valueOf(schemeRate.getGross()),
                                baseTxnId,
                                schemeRate,
                                product,
                                subProduct,
                                fromId,
                                commissionWalletType,
                                transactionDetailsInfo
                        );

                log.info("Processing commission | TransactionId={} | WalletType={}",
                        baseTxnId,
                        commissionWalletType);

                transactionEngineServiceImpl.processCommission(
                        dto,
                        baseTxnId,
                        "SUCCESS",
                        dto
                );

                log.info("Commission processed successfully.");
            }

        } catch (Exception e) {

            log.error("Error while processing charges or commission | TransactionId={}",
                    baseTxnId,
                    e);

            throw e;
        }

        log.info("Saving Fund Transfer record.");

        FundTransfer savedTransfer = saveFundTransferRecord(requestDto);

        log.info("Fund Transfer record saved successfully.");

        log.info("========== Fund Transfer Completed Successfully ==========");
        log.info("TransactionId={} | FromHierarchy={} | ToHierarchy={} | Amount={}",
                baseTxnId,
                fromId,
                toId,
                amount);

        return buildResponse(savedTransfer);
    }

    private void validateRequest(FundTransferRequestDto dto) {
        if (dto.getAmount() == null || dto.getConfirmAmount() == null) {
            throw new IllegalArgumentException("Amount fields must not be null.");
        }
        if (dto.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero.");
        }
        if (!dto.getAmount().equals(dto.getConfirmAmount())) {
            throw new IllegalArgumentException("Amount and confirm amount do not match.");
        }
    }

    private <T extends AbstractWalletAmountTable> void performWalletUpdate(
            T wallet, String hierarchyId, WalletType walletType, BigDecimal amount,
            TransactionType type, String baseTxnId, String service, String remarks,
            LocalDate date, LocalTime time, String subProductId, RetailersExtraColumnsDto retailerExtraColumns) {

        BigDecimal beforeBalance = wallet.getAvailableBalance();
        //BigDecimal afterBalance;

        if (type == TransactionType.CREDIT) {
            wallet.setTotalCash(wallet.getTotalCash().add(amount));
            wallet.setAvailableBalance(beforeBalance.add(amount));
        } else {
            wallet.setTotalCash(wallet.getTotalCash().subtract(amount));
            wallet.setAvailableBalance(beforeBalance.subtract(amount));
        }

        wallet.setLastUpdatedAmount(amount);
        wallet.setLastUpdatedDate(date);
        wallet.setLastUpdatedTime(time);

        String action = (type == TransactionType.CREDIT ? "Add" : "Deduct") + " Funds - " + walletType.name();

        insertTransaction(
                walletType.name(), hierarchyId, Status.SUCCESS.name(), amount,
                beforeBalance, wallet.getAvailableBalance(), action,
                ServiceType.FTR + baseTxnId, service, remarks, date, time, subProductId, retailerExtraColumns
        );
    }

    private void insertTransaction(String walletType, String hierarchyId, String status, BigDecimal amount,
                                   BigDecimal before, BigDecimal after, String action, String txnId,
                                   String service, String remarks, LocalDate date, LocalTime time, String subProductName, RetailersExtraColumnsDto retailerExtraColumns) {

        String hierarchyType = commonMethods.extractHierarchyType(hierarchyId);
        String tableName = commonMethods.getTransactionTable(hierarchyType, walletType);
        String columnName = commonMethods.getHierarchyColumnName(hierarchyType);

        boolean isRetailer = "RT".equalsIgnoreCase(hierarchyType);

        String extraColumns = "";
        String extraValues = "";

        if (isRetailer) {
            extraColumns = ", request_domain_name, client_ip, medium";
            extraValues = ", :requestDomainName, :clientIp, :medium";
        }

        String sql = "INSERT INTO " + tableName +
                " (" + columnName +
                ", service, amount, date_of_transaction, time_of_transaction, " +
                "bal_before_tran, bal_after_tran, action_on_balance_amount, final_balance, " +
                "transaction_id, tran_status, remarks, sub_product_name" +
                extraColumns + ") " +
                "VALUES (:hierarchyId, :service, :amount, :date, :time, :before, :after, :action, :final, " +
                ":txnId, :status, :remarks, :subProductName" +
                extraValues + ")";

        Query query = entityManager.createNativeQuery(sql)
                .setParameter("subProductName", subProductName)
                .setParameter("hierarchyId", hierarchyId)
                .setParameter("service", service)
                .setParameter("amount", amount)
                .setParameter("date", Date.valueOf(date))
                .setParameter("time", Time.valueOf(time))
                .setParameter("before", before)
                .setParameter("after", after)
                .setParameter("action", action)
                .setParameter("final", after)
                .setParameter("txnId", txnId)
                .setParameter("status", status)
                .setParameter("remarks", remarks);

        if (isRetailer) {
            query.setParameter("requestDomainName", retailerExtraColumns.getRequestDomainName())
                    .setParameter("clientIp", retailerExtraColumns.getClientIp())
                    .setParameter("medium", retailerExtraColumns.getMedium());
        }

        query.executeUpdate();
    }

    private FundTransfer saveFundTransferRecord(FundTransferRequestDto dto) {
        FundTransfer transfer = new FundTransfer();
        transfer.setFromHierarchyId(dto.getFromHierarchyId());
        transfer.setToHierarchyId(dto.getToHierarchyId());
        transfer.setAmount(dto.getAmount());
        transfer.setFromWalletType(dto.getFromWalletType());
        transfer.setToWalletType(dto.getToWalletType());
        transfer.setNarration(dto.getNarration());
        return fundTransferRepository.save(transfer);
    }

    private FundTransferResponseDto buildResponse(FundTransfer transfer) {
        return new FundTransferResponseDto(
                transfer.getId(),
                transfer.getFromHierarchyId(),
                transfer.getToHierarchyId(),
                transfer.getAmount(),
                transfer.getFromWalletType(),
                transfer.getToWalletType(),
                transfer.getTransferDate(),
                transfer.getNarration()
        );
    }


}
