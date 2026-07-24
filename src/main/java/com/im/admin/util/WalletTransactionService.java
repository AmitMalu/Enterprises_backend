package com.im.admin.util;

import com.im.admin.dto.TransactionDetailsInfoDto;
import com.im.admin.dto.TransactionSchemeDetailsRequestDto;
import com.im.admin.entities.SchemeRate;
import com.im.admin.entities.SubProducts;
import com.im.admin.entities.VendorRouting;
import com.im.admin.entities.enums.WalletType;
import com.im.admin.entities.product.Products;
import com.im.admin.entities.vendor.Vendors;
import com.im.admin.service.impl.TransactionEngineServiceImpl;
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

/**
 * @author SHUBHAM KHOPADE
 */
@Service
@Slf4j
public class WalletTransactionService {

    @Autowired
    private TransactionEngineServiceImpl transactionEngineServiceImpl;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private CommonMethods commonMethods;


    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processChargesForHierarchy(
            String deductedId,
            String creditedId,
            WalletType walletType,
            Products product,
            SubProducts subProduct,
            SchemeRate schemeRate,
            BigDecimal grossCharge,
            String baseTransactionId,
            TransactionDetailsInfoDto transactionDetailsInfo) {

        /* ================= COMMON CALC ================= */

        TransactionSchemeDetailsRequestDto dto =
                buildTransactionDto(
                        grossCharge,
                        baseTransactionId,
                        schemeRate,
                        product,
                        subProduct,
                        deductedId,
                        walletType,
                        transactionDetailsInfo
                );

        BigDecimal grossAmount =
                transactionEngineServiceImpl.calculateGross(dto, grossCharge);

        BigDecimal gstRate =
                BigDecimal.valueOf(product.getSgst())
                        .divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP);

        BigDecimal baseAmount =
                grossAmount.divide(BigDecimal.ONE.add(gstRate), 2, RoundingMode.HALF_UP);

        BigDecimal gstAmount =
                grossAmount.subtract(baseAmount).setScale(2, RoundingMode.HALF_UP);

        /* ================= DEBIT (RT / USER) ================= */

        String parentHierarchyType = extractHierarchyType(deductedId);

        String parentTable =
                transactionEngineServiceImpl.getTransactionTable(parentHierarchyType, walletType.name());

        String amountTable = commonMethods.getAmountTable(parentHierarchyType, walletType.name());

        // CHARGE DEBIT
        transactionEngineServiceImpl.updateAmountTable(parentHierarchyType, deductedId, amountTable, baseAmount.negate());

        BigDecimal afterChargeDebit =
                getUpdatedBalance(parentHierarchyType, deductedId, amountTable);

        transactionEngineServiceImpl.insertTransactionForHierarchy(
                parentTable,
                "DEBIT",
                baseAmount,
                "Charges Debited",
                deductedId,
                walletType.name(),
                afterChargeDebit.add(baseAmount), // before
                afterChargeDebit,
                "Deduct Funds (Charge)",
                "CHG" + baseTransactionId,
                "SUCCESS",
                dto
        );

        // GST DEBIT
        transactionEngineServiceImpl.updateAmountTable(parentHierarchyType, deductedId, amountTable, gstAmount.negate());

        BigDecimal afterGstDebit =
                getUpdatedBalance(parentHierarchyType, deductedId, amountTable);

        transactionEngineServiceImpl.insertTransactionForHierarchy(
                parentTable,
                "DEBIT",
                gstAmount,
                "GST Debited",
                deductedId,
                walletType.name(),
                afterGstDebit.add(gstAmount),
                afterGstDebit,
                "Deduct Funds (GST)",
                "GST" + baseTransactionId,
                "SUCCESS",
                dto
        );

        /* ================= CREDIT (WL / ADMIN) ================= */

        TransactionSchemeDetailsRequestDto dto2 =
                buildTransactionDto(
                        grossAmount,
                        baseTransactionId,
                        schemeRate,
                        product,
                        subProduct,
                        creditedId,
                        walletType,
                        transactionDetailsInfo

                );

        String hierarchyType2 = extractHierarchyType(creditedId);

        String tableName2 =
                transactionEngineServiceImpl.getTransactionTable(hierarchyType2, walletType.name());

        String amountTable2 = commonMethods.getAmountTable(hierarchyType2, walletType.name());


        // CHARGE CREDIT
        transactionEngineServiceImpl.updateAmountTable(hierarchyType2, creditedId, amountTable2, baseAmount);

        BigDecimal afterChargeCredit =
                getUpdatedBalance(hierarchyType2, creditedId, amountTable2);

        transactionEngineServiceImpl.insertTransactionForHierarchy(
                tableName2,
                "CREDIT",
                baseAmount,
                "Charges Credited",
                creditedId,
                walletType.name(),
                afterChargeCredit.subtract(baseAmount),
                afterChargeCredit,
                "Add Funds (Charge)",
                "CHG" + baseTransactionId,
                "SUCCESS",
                dto2
        );

        // GST CREDIT
        transactionEngineServiceImpl.updateAmountTable(hierarchyType2, creditedId, amountTable2, gstAmount);

        BigDecimal afterGstCredit =
                getUpdatedBalance(hierarchyType2, creditedId, amountTable2);

        transactionEngineServiceImpl.insertTransactionForHierarchy(
                tableName2,
                "CREDIT",
                gstAmount,
                "GST Credited",
                creditedId,
                walletType.name(),
                afterGstCredit.subtract(gstAmount),
                afterGstCredit,
                "Add Funds (GST)",
                "GST" + baseTransactionId,
                "SUCCESS",
                dto2
        );
    }


    public TransactionSchemeDetailsRequestDto buildTransactionDto(
            BigDecimal amount,
            String baseTransactionId,
            SchemeRate schemeRate,
            Products product,
            SubProducts subProduct,
            String hierarchyId,
            WalletType walletType,
            TransactionDetailsInfoDto transactionDetailsInfo) {

        TransactionSchemeDetailsRequestDto dto =
                new TransactionSchemeDetailsRequestDto();

        dto.setBaseTranxId(baseTransactionId);
        dto.setAmount(amount.doubleValue());
        dto.setConfirmAmount(amount.doubleValue());
        dto.setOperatorName(transactionDetailsInfo.getOperatorName());
        dto.setOperatorCode(schemeRate.getSchemeCode());
        dto.setSchemeRate(schemeRate);
        dto.setTransactionType(product.getTransactionType());
        dto.setWalletType(walletType.name());
        dto.setGstValue(String.valueOf(product.getSgst()));
        dto.setTdsRate(String.valueOf(product.getTds()));
        dto.setHierarchyId(hierarchyId);
        dto.setProductCode(product.getProductCode());
        dto.setSubProductCode(transactionDetailsInfo.getSubProductCode());
        dto.setIsCommission(product.getIsCommissions());
        dto.setHasCharge(product.getHasCharges());
        dto.setVendorName(transactionDetailsInfo.getVendorName());

        return dto;
    }

    public BigDecimal getUpdatedBalance(String hierarchyType, String hierarchyId, String tableName) {

        String columnName = getHierarchyColumnName(hierarchyType);

        String sql = "SELECT available_balance FROM " + tableName +
                " WHERE " + columnName + " = :hierarchyId";

        return (BigDecimal) entityManager.createNativeQuery(sql)
                .setParameter("hierarchyId", hierarchyId)
                .getSingleResult();
    }

    public String getHierarchyColumnName(String hierarchyType) {
        log.debug("Resolving column name for hierarchy type.");

        return switch (hierarchyType) {
            case "ET" -> "enterprises_id";
            case "PT" -> "partner_id";
            case "RT" -> "retailer_id";
            case "DT" -> "distributor_id";
            case "SD" -> "super_distributor_id";
            case "WL" -> "white_label_id";
            case "AD" -> "admin_id";
            default -> {
                log.warn("Invalid hierarchy type encountered.");
                throw new IllegalArgumentException("Unsupported hierarchy type.");
            }
        };
    }

    public String extractHierarchyType(String hierarchyId) {
        log.debug("Extracting hierarchy type from ID.");

        if (hierarchyId == null || hierarchyId.isEmpty()) {
            log.warn("Hierarchy ID is null or empty.");
            return "UNKNOWN";
        }

        String[] parts = hierarchyId.split("-");
        String type = (parts.length >= 2) ? parts[1] : "UNKNOWN";

        log.info("Extracted hierarchy type.");
        return type;
    }

    public static Vendors getVendors(VendorRouting vendorRouting) {
        Vendors vendor = null;

        if (vendorRouting.getRoutingPriority1() != null && vendorRouting.getRoutingPriority1().getActive()) {
            vendor = vendorRouting.getRoutingPriority1();
        } else if (vendorRouting.getRoutingPriority2() != null && vendorRouting.getRoutingPriority2().getActive()) {
            vendor = vendorRouting.getRoutingPriority2();
        } else if (vendorRouting.getRoutingPriority3() != null && vendorRouting.getRoutingPriority3().getActive()) {
            vendor = vendorRouting.getRoutingPriority3();
        }

        if (vendor == null) {
            throw new RuntimeException("No active vendor found for this subProductId");
        }
        return vendor;
    }

    public void processHierarchyCharges(
            String hierarchyId,
            Products product,
            SubProducts subProduct,
            SchemeRate schemeRate,
            BigDecimal grossCharge,
            String baseTransactionId,
            TransactionDetailsInfoDto transactionDetailsInfo) {

        String originalId = hierarchyId;
        String currentId = hierarchyId;

        WalletType walletType = "POSTPAID".equalsIgnoreCase(subProduct.getWalletType())
                ? WalletType.POSTPAID
                : WalletType.PREPAID;

        while (true) {

            Optional<String> parentOpt = commonMethods.findParentHierarchy(currentId);

            if (parentOpt.isEmpty()) {
                throw new RuntimeException("No approver found for hierarchy: " + currentId);
            }

            String parentId = parentOpt.get();
            String parentType = commonMethods.extractHierarchyType(parentId);

            if (isChargeableParent(parentType)) {

                processChargesForHierarchy(
                        originalId,
                        parentId,
                        walletType,
                        product,
                        subProduct,
                        schemeRate,
                        grossCharge,
                        baseTransactionId,
                        transactionDetailsInfo
                );

                break;
            }

            currentId = parentId;
        }
    }

    private boolean isChargeableParent(String type) {
        return "WL".equals(type) || "IM".equals(type) || "AD".equals(type);
    }
}
