package com.im.admin.util;

import com.im.admin.dto.AbstractWalletAmountTable;
import com.im.admin.entities.TransactionSequence;
import com.im.admin.entities.VendorRouting;
import com.im.admin.entities.enums.WalletType;
import com.im.admin.entities.vendor.Vendors;
import com.im.admin.repositories.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.sql.Time;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Date;
import java.util.Optional;

@Component
@Slf4j
public class CommonMethods {
    @Autowired
    private RetailerUserRepository retailerUserRepository;
    @Autowired
    private EnterprisesPostpaidAmountRepository enterprisesPostpaidAmountRepository;

    @Autowired
    private EnterprisesPrepaidAmountRepository enterprisesPrepaidAmountRepository;

    @Autowired
    private SuperDistributorPostpaidAmountRepository superDistributorPostpaidAmountRepository;

    @Autowired
    private SuperDistributorPrepaidAmountRepository superDistributorPrepaidAmountRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private RetailerRepository retailerRepository;

    @Autowired
    private RetailerPostpaidAmountRepository retailerPostpaidAmountRepository;

    @Autowired
    private RetailerPrepaidAmountRepository retailerPrepaidAmountRepository;

    @Autowired
    private DistributorRepository distributorRepository;

    @Autowired
    private DistributorPostpaidAmountRepository distributorPostpaidAmountRepository;

    @Autowired
    private DistributorPrepaidAmountRepository distributorPrepaidAmountRepository;

    @Autowired
    private PartnerRepository partnerRepository;

    @Autowired
    private PartnerPostpaidAmountRepository partnerPostpaidAmountRepository;

    @Autowired
    private PartnerPrepaidAmountRepository partnerPrepaidAmountRepository;

    @Autowired
    private SuperDistributorRepository superDistributorRepository;

    @Autowired
    private WhiteLabelRepository whiteLabelsRepository;

    @Autowired
    private WhiteLabelPrepaidAmountRepository whiteLabelPrepaidAmountRepository;

    @Autowired
    private WhiteLabelPostpaidAmountRepository whiteLabelPostpaidAmountRepository;

    @Autowired
    private EnterprisesRepository enterprisesRepository;

    @Autowired
    private AdminPostpaidAmountRepository adminPostpaidAmountRepository;

    @Autowired
    private AdminPrepaidAmountRepository adminPrepaidAmountRepository;

    @Autowired
    private TransactionSequenceRepository transactionSequenceRepository;

    public String generateNextIdForTables(String tableName, String idColumn) {

        String query = "SELECT MAX(" + idColumn + ") FROM " + tableName;
        String maxId = jdbcTemplate.queryForObject(query, String.class);

        if(maxId == null) {
            return "01";
        }

        int nextId = Integer.parseInt(maxId) + 1;
        if(nextId > 99) {
            throw new IllegalStateException("ID limit reached. Cannot exceed 99.");
        }

        return String.format("%02d", nextId); // Format as "01", "02", ..., "99"

    }

    public String generateNextSequentialId(String lastId) {

        if (lastId == null) {
            return "001";
        }

        int nextId = Integer.parseInt(lastId) + 1;

        if (nextId > 999) {
            return null;
        }

        return String.format("%03d", nextId);
    }

    public String generateBaseTransactionId() {
        String timestamp = new SimpleDateFormat("yyMMddHHmmss").format(new Date());
        long sequence = transactionSequenceRepository.save(new TransactionSequence()).getId();

        if(sequence == 99999) {
            jdbcTemplate.execute("TRUNCATE TABLE transaction_sequence");
        }

        return timestamp + String.format("%05d", sequence);
    }

    private int getNextTransactionSequence() {
        jdbcTemplate.update("INSERT INTO transaction_sequence () VALUES ()");
        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Integer.class);
    }

    public String getAmountTable(String hierarchyType, String walletType) {
        String entityName = getEntityName(hierarchyType);
        return entityName + (walletType.equalsIgnoreCase("PREPAID") ? "_prepaid_amount" : "_postpaid_amount");
    }

    public String extractHierarchyType(String hierarchyId) {
        String[] parts = hierarchyId.split("-");
        return (parts.length >= 2) ? parts[1] : "UNKNOWN";
    }

    public String getTransactionTable(String hierarchyType, String walletType) {
        String entityName = getEntityName(hierarchyType);
        return entityName + (walletType.equalsIgnoreCase("PREPAID") ? "_prepaid_transaction_details" : "_postpaid_transaction_details");
    }

    public String getEntityName(String hierarchyType) {
        return switch (hierarchyType) {
            case "AD" -> "admin";
            case "ET" -> "enterprises";
            case "WL" -> "white_label";
            case "SD" -> "super_distributor";
            case "PT" -> "partner";
            case "DT" -> "distributor";
            case "RT" -> "retailer";
            case "RU" -> "retailer_user";
            default -> throw new IllegalArgumentException("Invalid hierarchy type: " + hierarchyType);
        };
    }

    public BigDecimal getCurrentBalance(String hierarchyId, String walletType) {
        String amountTable = getAmountTable(extractHierarchyType(hierarchyId), walletType);
        String columnName = getHierarchyColumnName(extractHierarchyType(hierarchyId));

        String sql = "SELECT available_balance FROM " + amountTable + " WHERE " + columnName + " = :hierarchyId";

        try {
            return (BigDecimal) entityManager.createNativeQuery(sql)
                    .setParameter("hierarchyId", hierarchyId)
                    .getSingleResult();
        } catch (NoResultException e) {
            log.warn("No balance record found for Hierarchy ID: {} in table: {}", hierarchyId, amountTable);
            return BigDecimal.ZERO; // Default balance if no record exists
        }
    }

    public String getHierarchyColumnName(String hierarchyType) {
        return switch (hierarchyType) {
            case "ET" -> "enterprises_id";
            case "PT" -> "partner_id";
            case "RT" -> "retailer_id";
            case "DT" -> "distributor_id";
            case "SD" -> "super_distributor_id";
            case "WL" -> "white_label_id";
            case "AD" -> "admin_id";
            default -> throw new IllegalArgumentException("Invalid hierarchy type: " + hierarchyType);
        };
    }

    public Optional<String> findParentHierarchy(String hierarchyId) {
        String hierarchyType = extractHierarchyType(hierarchyId);

        // Stop processing if the hierarchy is "Admin"
        if ("AD".equals(hierarchyType)) {
            log.info("Reached Admin level. No further processing.");
            return Optional.empty();
        }

        Optional<?> entity = switch (hierarchyType) {
            case "ET" -> enterprisesRepository.findById(hierarchyId);
            case "WL" -> whiteLabelsRepository.findById(hierarchyId);
            case "SD" -> superDistributorRepository.findById(hierarchyId);
            case "PT" -> partnerRepository.findById(hierarchyId);
            case "DT" -> distributorRepository.findById(hierarchyId);
            case "RT" -> retailerRepository.findById(hierarchyId);
            case "RU" -> retailerUserRepository.findById(hierarchyId);
            default -> Optional.empty();
        };

        return entity.map(this::extractParentAccount);
    }

    public String extractParentAccount(Object entity) {
        try {
            Field parentField = entity.getClass().getDeclaredField("parentAccount");
            parentField.setAccessible(true);
            return (String) parentField.get(entity);
        } catch (Exception e) {
            log.error("Error extracting parent account from entity: {}", entity.getClass().getSimpleName(), e);
            return null;
        }
    }

    public AbstractWalletAmountTable getWalletByType(String walletType, String hierarchyId) {
        String hierarchyType = extractHierarchyType(hierarchyId); // e.g. "RT", "DT", "PT", "SD", "WL", "ET", "AD", etc.

        return switch (hierarchyType) {
            case "RT" -> getRetailerWallet(walletType, hierarchyId);
            case "DT" -> getDistributorWallet(walletType, hierarchyId);
            case "PT" -> getPartnerWallet(walletType, hierarchyId);
            case "SD" -> getSuperDistributorWallet(walletType, hierarchyId);
            case "WL" -> getWhiteLabelWallet(walletType, hierarchyId);
            case "ET" -> getEnterprisesWallet(walletType, hierarchyId);
            case "AD", "IM" -> getAdminWallet(walletType, hierarchyId);
            default -> throw new IllegalArgumentException("Unsupported hierarchy for wallet: " + hierarchyId);
        };
    }

    private AbstractWalletAmountTable getRetailerWallet(String walletType, String retailerId) {
        WalletType type = WalletType.valueOf(walletType.toUpperCase()); // Convert to enum safely

        if (type == WalletType.PREPAID) {
            return retailerPrepaidAmountRepository.findByRetailerId_RetailerId(retailerId)
                    .orElseThrow(() -> new RuntimeException("Retailer Prepaid wallet not found: " + retailerId));
        } else if (type== WalletType.POSTPAID) {
            return retailerPostpaidAmountRepository.findByRetailerId_RetailerId(retailerId)
                    .orElseThrow(() -> new RuntimeException("Retailer Postpaid wallet not found: " + retailerId));
        }
        throw new IllegalArgumentException("Invalid wallet type for Retailer: " + walletType);
    }

    private AbstractWalletAmountTable getDistributorWallet(String walletType, String distributorId) {
        WalletType type = WalletType.valueOf(walletType.toUpperCase()); // Convert to enum safely

        if (type == WalletType.PREPAID) {
            return distributorPrepaidAmountRepository.findByDistributorId_DistributorId(distributorId)
                    .orElseThrow(() -> new RuntimeException("Distributor Prepaid wallet not found: " + distributorId));
        } else if (type == WalletType.POSTPAID) {
            return distributorPostpaidAmountRepository.findByDistributorId_DistributorId(distributorId)
                    .orElseThrow(() -> new RuntimeException("Distributor Postpaid wallet not found: " + distributorId));
        }
        throw new IllegalArgumentException("Invalid wallet type for Distributor: " + walletType);
    }

    private AbstractWalletAmountTable getPartnerWallet(String walletType, String partnerId) {
        WalletType type = WalletType.valueOf(walletType.toUpperCase()); // Convert to enum safely

        if (type == WalletType.PREPAID) {
            return partnerPrepaidAmountRepository.findByPartnerId_PartnerId(partnerId)
                    .orElseThrow(() -> new RuntimeException("Partner Prepaid wallet not found: " + partnerId));
        } else if (type == WalletType.POSTPAID) {
            return partnerPostpaidAmountRepository.findByPartnerId_PartnerId(partnerId)
                    .orElseThrow(() -> new RuntimeException("Partner Postpaid wallet not found: " + partnerId));
        }
        throw new IllegalArgumentException("Invalid wallet type for Partner: " + walletType);
    }

    private AbstractWalletAmountTable getSuperDistributorWallet(String walletType, String superDistributorId) {
        WalletType type = WalletType.valueOf(walletType.toUpperCase()); // Convert to enum safely

        if (type == WalletType.PREPAID) {
            return superDistributorPrepaidAmountRepository.findBySuperDistributorId_SuperDistributorId(superDistributorId)
                    .orElseThrow(() -> new RuntimeException("Super Distributor Prepaid wallet not found: " + superDistributorId));
        } else if (type == WalletType.POSTPAID) {
            return superDistributorPostpaidAmountRepository.findBySuperDistributorId_SuperDistributorId(superDistributorId)
                    .orElseThrow(() -> new RuntimeException("Super Distributor Postpaid wallet not found: " + superDistributorId));
        }
        throw new IllegalArgumentException("Invalid wallet type for Super Distributor: " + walletType);
    }

    private AbstractWalletAmountTable getWhiteLabelWallet(String walletType, String whiteLabelId) {
        WalletType type = WalletType.valueOf(walletType.toUpperCase()); // Convert to enum safely

        if (type == WalletType.PREPAID) {
            return whiteLabelPrepaidAmountRepository.findByWhiteLabelId_WhiteLabelId(whiteLabelId)
                    .orElseThrow(() -> new RuntimeException("WhiteLabel Prepaid wallet not found: " + whiteLabelId));
        } else if (type == WalletType.POSTPAID) {
            return whiteLabelPostpaidAmountRepository.findByWhiteLabelId_WhiteLabelId(whiteLabelId)
                    .orElseThrow(() -> new RuntimeException("WhiteLabel Postpaid wallet not found: " + whiteLabelId));
        }
        throw new IllegalArgumentException("Invalid wallet type for WhiteLabel: " + walletType);
    }

    private AbstractWalletAmountTable getEnterprisesWallet(String walletType, String enterprisesId) {
        WalletType type = WalletType.valueOf(walletType.toUpperCase()); // Convert to enum safely

        if (type == WalletType.PREPAID) {
            return enterprisesPrepaidAmountRepository.findByEnterprise_EnterpriseId(enterprisesId)
                    .orElseThrow(() -> new RuntimeException("Enterprises Prepaid wallet not found: " + enterprisesId));
        } else if (type == WalletType.POSTPAID) {
            return enterprisesPostpaidAmountRepository.findByEnterprise_EnterpriseId(enterprisesId)
                    .orElseThrow(() -> new RuntimeException("Enterprises Postpaid wallet not found: " + enterprisesId));
        }
        throw new IllegalArgumentException("Invalid wallet type for Enterprises: " + walletType);
    }

    private AbstractWalletAmountTable getAdminWallet(String walletType, String adminId) {
        WalletType type = WalletType.valueOf(walletType.toUpperCase()); // Convert to enum safely

        if (type == WalletType.PREPAID) {
            return adminPrepaidAmountRepository.findByAdminId_Id(adminId)
                    .orElseThrow(() -> new RuntimeException("Admin Prepaid wallet not found: " + adminId));
        } else if (type == WalletType.POSTPAID) {
            return adminPostpaidAmountRepository.findByAdminId_Id(adminId)
                    .orElseThrow(() -> new RuntimeException("Admin Postpaid wallet not found: " + adminId));
        }
        throw new IllegalArgumentException("Invalid wallet type for Admin: " + walletType);
    }

    public <T extends AbstractWalletAmountTable> void updateWallet(T wallet, String hierarchyId, WalletType walletType, BigDecimal amount,
                                                                    String tranxType, String baseTransactionId, String tranStatus, String service) {

        String actionOnBalance;

        // *** Credit or Debit Operation ***
        BigDecimal balanceBefore = wallet.getAvailableBalance();

        if (tranxType.equalsIgnoreCase("credit")) {
            wallet.setTotalCash(wallet.getTotalCash().add(amount));
            wallet.setAvailableBalance(wallet.getAvailableBalance().add(amount));
            actionOnBalance = "Add Funds - " + walletType.name();
        } else {
            wallet.setTotalCash(wallet.getTotalCash().subtract(amount));
            wallet.setAvailableBalance(wallet.getAvailableBalance().subtract(amount));
            actionOnBalance = "Deduct Funds - " +  walletType.name();
        }

        wallet.setLastUpdatedAmount(amount);
        wallet.setLastUpdatedDate(LocalDate.now());
        wallet.setLastUpdatedTime(LocalTime.now());

        // Insert transaction
        insertTransaction(walletType.name(), hierarchyId, tranStatus, amount, balanceBefore,
                wallet.getAvailableBalance(), actionOnBalance, "TXN" + baseTransactionId, service);

    }


    private void insertTransaction(String walletType, String hierarchyId, String tranStatus, BigDecimal amount,
                                   BigDecimal beforeBalance, BigDecimal afterBalance, String actionOnBalanceAmount,
                                   String tranxId, String service) {

        String hierarchyType = extractHierarchyType(hierarchyId);
        String transactionTableName = getTransactionTable(hierarchyType, walletType);
        String columnName = getHierarchyColumnName(hierarchyType);

        log.info("Inserting into {} | Type: {} | Amount: {} | Hierarchy ID: {} | Before Balance: {} | After Balance: {}",
                transactionTableName, service, amount, hierarchyId, beforeBalance, afterBalance);

        String sql = "INSERT INTO " + transactionTableName +
                " (" + columnName + ",service, amount, date_of_transaction, time_of_transaction, " +
                "bal_before_tran, bal_after_tran, action_on_balance_amount, final_balance, transaction_id, tran_status) " +
                "VALUES (:hierarchyId, :service, :amount, :dateOfTransaction, :timeOfTransaction, " +
                ":beforeTransactionBalance, :afterTransactionBalance, :actionOnBalanceAmount, :finalBalance, :tranxId, :tranStatus)";

        try {
            Query query = entityManager.createNativeQuery(sql)
                    .setParameter("hierarchyId", hierarchyId)
                    .setParameter("service", service)
                    .setParameter("amount", amount)
                    .setParameter("dateOfTransaction", LocalDate.now()) // use LocalDate
                    .setParameter("timeOfTransaction", LocalTime.now()) // use LocalTime
                    .setParameter("beforeTransactionBalance", beforeBalance)
                    .setParameter("afterTransactionBalance", afterBalance)
                    .setParameter("actionOnBalanceAmount", actionOnBalanceAmount)
                    .setParameter("finalBalance", afterBalance)
                    .setParameter("tranxId", tranxId)
                    .setParameter("tranStatus", tranStatus);

            query.executeUpdate();

        } catch (Exception e) {
            log.error("Error inserting transaction: {}", e.getMessage(), e);
            throw new RuntimeException("Transaction insert failed", e);
        }
    }

    public static Vendors getVendors(VendorRouting vendorRouting) {
        if (vendorRouting == null) {
            throw new IllegalArgumentException("vendorRouting is null");
        }

        if (vendorRouting.getRoutingPriority1() != null && vendorRouting.getRoutingPriority1().getActive()) {
            return vendorRouting.getRoutingPriority1();
        } else if (vendorRouting.getRoutingPriority2() != null && vendorRouting.getRoutingPriority2().getActive()) {
            return vendorRouting.getRoutingPriority2();
        } else if (vendorRouting.getRoutingPriority3() != null && vendorRouting.getRoutingPriority3().getActive()) {
            return vendorRouting.getRoutingPriority3();
        }

        throw new RuntimeException("No active vendor found for this subProductId");
    }

}