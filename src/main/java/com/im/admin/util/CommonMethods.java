package com.im.admin.util;

import com.im.admin.dto.AbstractWalletAmountTable;
import com.im.admin.dto.BillRechargeResponseDTO;
import com.im.admin.entities.RetailerUser;
import com.im.admin.entities.SchemeRate;
import com.im.admin.entities.TransactionSequence;
import com.im.admin.entities.VendorRouting;
import com.im.admin.entities.enums.ServiceType;
import com.im.admin.entities.enums.Status;
import com.im.admin.entities.enums.TransactionType;
import com.im.admin.entities.enums.WalletType;
import com.im.admin.entities.vendor.Vendors;
import com.im.admin.repositories.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.RoundingMode;
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
        log.debug("Generating next ID for table.");

        try {
            String query = "SELECT MAX(" + idColumn + ") FROM " + tableName;
            log.trace("Executing query to fetch max ID."); // Avoid logging actual query in production

            String maxId = jdbcTemplate.queryForObject(query, String.class);

            if (maxId == null) {
                log.info("No existing ID found. Starting from default.");
                return "01";
            }

            int nextId = Integer.parseInt(maxId) + 1;

            if (nextId > 99) {
                log.error("ID generation failed: limit exceeded.");
                throw new IllegalStateException("ID limit reached.");
            }

            String formattedId = String.format("%02d", nextId);
            log.debug("Generated next ID successfully.");
            return formattedId;

        } catch (NumberFormatException e) {
            log.error("Failed to parse max ID value.", e);
            throw new IllegalStateException("Invalid ID format in database.");
        } catch (Exception e) {
            log.error("Unexpected error during ID generation.", e);
            throw new RuntimeException("Failed to generate next ID.");
        }
    }

    public String generateNextSequentialId(String lastId) {
        log.debug("Generating next sequential ID.");

        try {
            if (lastId == null) {
                log.info("No previous ID found. Starting from default.");
                return "001";
            }

            int nextId = Integer.parseInt(lastId) + 1;

            if (nextId > 999) {
                log.warn("Sequential ID limit reached.");
                return null;
            }

            String formattedId = String.format("%03d", nextId);
            log.debug("Generated sequential ID successfully.");
            return formattedId;

        } catch (NumberFormatException e) {
            log.error("Invalid format for last ID: {}", lastId);
            throw new IllegalArgumentException("Last ID is not a valid number.");
        }
    }

    public String generateBaseTransactionId() {
        String timestamp = new SimpleDateFormat("yyMMddHHmmss").format(new Date());
        log.debug("Generating base transaction ID with timestamp: {}", timestamp);

        try {
            long sequence = transactionSequenceRepository.save(new TransactionSequence()).getId();
            log.debug("Retrieved sequence number for transaction ID.");

            if (sequence == 99999) {
                log.warn("Sequence limit reached. Resetting transaction sequence table.");
                jdbcTemplate.execute("TRUNCATE TABLE transaction_sequence");
            }

            String transactionId = timestamp + String.format("%05d", sequence);
            log.info("Generated base transaction ID.");
            return transactionId;

        } catch (Exception e) {
            log.error("Error generating base transaction ID.", e);
            throw new RuntimeException("Failed to generate transaction ID.");
        }
    }

    private int getNextTransactionSequence() {
        jdbcTemplate.update("INSERT INTO transaction_sequence () VALUES ()");
        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Integer.class);
    }

    public String getAmountTable(String hierarchyType, String walletType) {
        log.debug("Resolving amount table for hierarchyType and walletType.");

        try {
            String entityName = getEntityName(hierarchyType);
            String suffix = walletType.equalsIgnoreCase("PREPAID") ? "_prepaid_amount" : "_postpaid_amount";
            String tableName = entityName + suffix;

            log.info("Resolved amount table name.");
            return tableName;

        } catch (Exception e) {
            log.error("Error resolving amount table.", e);
            throw new RuntimeException("Failed to determine amount table.");
        }
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

    public String getTransactionTable(String hierarchyType, String walletType) {
        log.debug("Resolving transaction table name based on hierarchy and wallet type.");

        try {
            String entityName = getEntityName(hierarchyType);
            String suffix = walletType.equalsIgnoreCase("PREPAID")
                    ? "_prepaid_transaction_details"
                    : "_postpaid_transaction_details";

            String tableName = entityName + suffix;
            log.info("Transaction table resolved successfully.");
            return tableName;

        } catch (Exception e) {
            log.error("Failed to resolve transaction table.", e);
            throw new RuntimeException("Unable to determine transaction table.");
        }
    }

    public String getEntityName(String hierarchyType) {
        log.debug("Mapping hierarchy type to entity name.");

        return switch (hierarchyType) {
            case "AD" -> "admin";
            case "ET" -> "enterprises";
            case "WL" -> "white_label";
            case "SD" -> "super_distributor";
            case "PT" -> "partner";
            case "DT" -> "distributor";
            case "RT" -> "retailer";
            case "RU" -> "retailer_user";
            default -> {
                log.warn("Unrecognized hierarchy type encountered.");
                throw new IllegalArgumentException("Invalid hierarchy type provided.");
            }
        };
    }

    public BigDecimal getCurrentBalance(String hierarchyId, String walletType) {
        log.debug("Initiating balance retrieval for wallet type: {}", walletType);

        try {
            String hierarchyType = extractHierarchyType(hierarchyId);

            String resolvedHierarchyId = hierarchyId;
            String resolvedHierarchyType = hierarchyType;

            // 🔹 Special handling for RetailerUser
            if ("RU".equalsIgnoreCase(hierarchyType)) {
                log.debug("Hierarchy is RetailerUser. Resolving parent Retailer ID.");

                RetailerUser retailerUser = retailerUserRepository.findByIdWithRetailer(hierarchyId)
                        .orElseThrow(() -> new RuntimeException("RetailerUser not found with ID: " + hierarchyId));

                resolvedHierarchyId = retailerUser.getRetailer().getRetailerId();
                resolvedHierarchyType = "RT";
            }

            String amountTable = getAmountTable(resolvedHierarchyType, walletType);
            String columnName = getHierarchyColumnName(resolvedHierarchyType);

            log.trace("Preparing SQL query for balance lookup: {}", amountTable);

            String sql = "SELECT available_balance FROM " + amountTable + " WHERE " + columnName + " = :hierarchyId";

            BigDecimal balance = (BigDecimal) entityManager.createNativeQuery(sql)
                    .setParameter("hierarchyId", resolvedHierarchyId)  // use final variable
                    .getSingleResult();

            log.info("Balance retrieved successfully.");
            return balance;

        } catch (NoResultException e) {
            log.warn("No balance record found for provided hierarchy.");
            return BigDecimal.ZERO;
        } catch (Exception e) {
            log.error("Error occurred while retrieving balance.", e);
            throw new RuntimeException("Unable to fetch current balance.");
        }
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

    public Optional<String> findParentHierarchy(String hierarchyId) {
        log.debug("Initiating parent hierarchy lookup.");

        String hierarchyType = extractHierarchyType(hierarchyId);
        log.debug("Extracted hierarchy type: {}", hierarchyType);

        if ("AD".equals(hierarchyType)) {
            log.info("Hierarchy type is Admin. No parent exists.");
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
            default -> {
                log.warn("Unrecognized hierarchy type. Cannot resolve parent.");
                yield Optional.empty();
            }
        };

        if (entity.isEmpty()) {
            log.warn("No entity found for hierarchy ID.");
        }

        return entity.map(this::extractParentAccount);
    }

    public String extractParentAccount(Object entity) {
        try {
            Object unproxied = Hibernate.unproxy(entity);
            Field parentField = unproxied.getClass().getDeclaredField("parentAccount");
            parentField.setAccessible(true);
            return (String) parentField.get(unproxied);
        } catch (NoSuchFieldException e) {
            log.error("Entity class does not contain 'parentAccount' field: {}", entity.getClass().getSimpleName());
        } catch (IllegalAccessException e) {
            log.error("Access denied while retrieving parent account from entity: {}", entity.getClass().getSimpleName());
        } catch (Exception e) {
            log.error("Unexpected error during parent account extraction.", e);
        }
        return null;
    }

    public AbstractWalletAmountTable getWalletByType(String walletType, String hierarchyId) {
        log.debug("Resolving wallet by type for hierarchy.");

        String hierarchyType = extractHierarchyType(hierarchyId);
        log.debug("Extracted hierarchy type: {}", hierarchyType);

        return switch (hierarchyType) {
            case "RT" -> getRetailerWallet(walletType, hierarchyId);
            case "DT" -> getDistributorWallet(walletType, hierarchyId);
            case "PT" -> getPartnerWallet(walletType, hierarchyId);
            case "SD" -> getSuperDistributorWallet(walletType, hierarchyId);
            case "WL" -> getWhiteLabelWallet(walletType, hierarchyId);
            case "ET" -> getEnterprisesWallet(walletType, hierarchyId);
            case "AD", "IM" -> getAdminWallet(walletType, hierarchyId);
            default -> {
                log.warn("Unsupported hierarchy type encountered.");
                throw new IllegalArgumentException("Unsupported hierarchy type for wallet resolution.");
            }
        };
    }

    private AbstractWalletAmountTable getRetailerWallet(String walletType, String retailerId) {
        log.debug("Fetching retailer wallet for type.");

        WalletType type;
        try {
            type = WalletType.valueOf(walletType.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.error("Invalid wallet type provided for retailer.");
            throw new IllegalArgumentException("Unsupported wallet type.");
        }

        try {
            if (type == WalletType.PREPAID) {
                return retailerPrepaidAmountRepository.findByRetailerId_RetailerId(retailerId)
                        .orElseThrow(() -> {
                            log.warn("Retailer prepaid wallet not found.");
                            return new RuntimeException("Retailer prepaid wallet unavailable.");
                        });
            } else if (type == WalletType.POSTPAID) {
                return retailerPostpaidAmountRepository.findByRetailerId_RetailerId(retailerId)
                        .orElseThrow(() -> {
                            log.warn("Retailer postpaid wallet not found.");
                            return new RuntimeException("Retailer postpaid wallet unavailable.");
                        });
            } else {
                log.error("Unhandled wallet type for retailer.");
                throw new IllegalArgumentException("Invalid wallet type for retailer.");
            }
        } catch (Exception e) {
            log.error("Error retrieving retailer wallet.", e);
            throw new RuntimeException("Failed to retrieve retailer wallet.");
        }
    }

    private AbstractWalletAmountTable getDistributorWallet(String walletType, String distributorId) {
        log.debug("Fetching distributor wallet for type.");

        WalletType type;
        try {
            type = WalletType.valueOf(walletType.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.error("Invalid wallet type provided for distributor.");
            throw new IllegalArgumentException("Unsupported wallet type.");
        }

        try {
            if (type == WalletType.PREPAID) {
                return distributorPrepaidAmountRepository.findByDistributorId_DistributorId(distributorId)
                        .orElseThrow(() -> {
                            log.warn("Distributor prepaid wallet not found.");
                            return new RuntimeException("Distributor prepaid wallet unavailable.");
                        });
            } else if (type == WalletType.POSTPAID) {
                return distributorPostpaidAmountRepository.findByDistributorId_DistributorId(distributorId)
                        .orElseThrow(() -> {
                            log.warn("Distributor postpaid wallet not found.");
                            return new RuntimeException("Distributor postpaid wallet unavailable.");
                        });
            } else {
                log.error("Unhandled wallet type for distributor.");
                throw new IllegalArgumentException("Invalid wallet type for distributor.");
            }
        } catch (Exception e) {
            log.error("Error retrieving distributor wallet.", e);
            throw new RuntimeException("Failed to retrieve distributor wallet.");
        }
    }

    private AbstractWalletAmountTable getPartnerWallet(String walletType, String partnerId) {
        log.debug("Fetching partner wallet for type.");

        WalletType type;
        try {
            type = WalletType.valueOf(walletType.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.error("Invalid wallet type provided for partner.");
            throw new IllegalArgumentException("Unsupported wallet type.");
        }

        try {
            if (type == WalletType.PREPAID) {
                return partnerPrepaidAmountRepository.findByPartnerId_PartnerId(partnerId)
                        .orElseThrow(() -> {
                            log.warn("Partner prepaid wallet not found.");
                            return new RuntimeException("Partner prepaid wallet unavailable.");
                        });
            } else if (type == WalletType.POSTPAID) {
                return partnerPostpaidAmountRepository.findByPartnerId_PartnerId(partnerId)
                        .orElseThrow(() -> {
                            log.warn("Partner postpaid wallet not found.");
                            return new RuntimeException("Partner postpaid wallet unavailable.");
                        });
            } else {
                log.error("Unhandled wallet type for partner.");
                throw new IllegalArgumentException("Invalid wallet type for partner.");
            }
        } catch (Exception e) {
            log.error("Error retrieving partner wallet.", e);
            throw new RuntimeException("Failed to retrieve partner wallet.");
        }
    }

    private AbstractWalletAmountTable getSuperDistributorWallet(String walletType, String superDistributorId) {
        log.debug("Fetching super distributor wallet for type.");

        WalletType type;
        try {
            type = WalletType.valueOf(walletType.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.error("Invalid wallet type provided for super distributor.");
            throw new IllegalArgumentException("Unsupported wallet type.");
        }

        try {
            if (type == WalletType.PREPAID) {
                return superDistributorPrepaidAmountRepository.findBySuperDistributorId_SuperDistributorId(superDistributorId)
                        .orElseThrow(() -> {
                            log.warn("Super distributor prepaid wallet not found.");
                            return new RuntimeException("Super distributor prepaid wallet unavailable.");
                        });
            } else if (type == WalletType.POSTPAID) {
                return superDistributorPostpaidAmountRepository.findBySuperDistributorId_SuperDistributorId(superDistributorId)
                        .orElseThrow(() -> {
                            log.warn("Super distributor postpaid wallet not found.");
                            return new RuntimeException("Super distributor postpaid wallet unavailable.");
                        });
            } else {
                log.error("Unhandled wallet type for super distributor.");
                throw new IllegalArgumentException("Invalid wallet type for super distributor.");
            }
        } catch (Exception e) {
            log.error("Error retrieving super distributor wallet.", e);
            throw new RuntimeException("Failed to retrieve super distributor wallet.");
        }
    }

    private AbstractWalletAmountTable getWhiteLabelWallet(String walletType, String whiteLabelId) {
        log.debug("Fetching white label wallet for type.");

        WalletType type;
        try {
            type = WalletType.valueOf(walletType.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.error("Invalid wallet type provided for white label.");
            throw new IllegalArgumentException("Unsupported wallet type.");
        }

        try {
            if (type == WalletType.PREPAID) {
                return whiteLabelPrepaidAmountRepository.findByWhiteLabelId_WhiteLabelId(whiteLabelId)
                        .orElseThrow(() -> {
                            log.warn("White label prepaid wallet not found.");
                            return new RuntimeException("White label prepaid wallet unavailable.");
                        });
            } else if (type == WalletType.POSTPAID) {
                return whiteLabelPostpaidAmountRepository.findByWhiteLabelId_WhiteLabelId(whiteLabelId)
                        .orElseThrow(() -> {
                            log.warn("White label postpaid wallet not found.");
                            return new RuntimeException("White label postpaid wallet unavailable.");
                        });
            } else {
                log.error("Unhandled wallet type for white label.");
                throw new IllegalArgumentException("Invalid wallet type for white label.");
            }
        } catch (Exception e) {
            log.error("Error retrieving white label wallet.", e);
            throw new RuntimeException("Failed to retrieve white label wallet.");
        }
    }

    private AbstractWalletAmountTable getEnterprisesWallet(String walletType, String enterprisesId) {
        log.debug("Fetching enterprises wallet for type.");

        WalletType type;
        try {
            type = WalletType.valueOf(walletType.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.error("Invalid wallet type provided for enterprises.");
            throw new IllegalArgumentException("Unsupported wallet type.");
        }

        try {
            if (type == WalletType.PREPAID) {
                return enterprisesPrepaidAmountRepository.findByEnterprise_EnterpriseId(enterprisesId)
                        .orElseThrow(() -> {
                            log.warn("Enterprises prepaid wallet not found.");
                            return new RuntimeException("Enterprises prepaid wallet unavailable.");
                        });
            } else if (type == WalletType.POSTPAID) {
                return enterprisesPostpaidAmountRepository.findByEnterprise_EnterpriseId(enterprisesId)
                        .orElseThrow(() -> {
                            log.warn("Enterprises postpaid wallet not found.");
                            return new RuntimeException("Enterprises postpaid wallet unavailable.");
                        });
            } else {
                log.error("Unhandled wallet type for enterprises.");
                throw new IllegalArgumentException("Invalid wallet type for enterprises.");
            }
        } catch (Exception e) {
            log.error("Error retrieving enterprises wallet.", e);
            throw new RuntimeException("Failed to retrieve enterprises wallet.");
        }
    }

    private AbstractWalletAmountTable getAdminWallet(String walletType, String adminId) {
        log.debug("Fetching admin wallet for type.");

        WalletType type;
        try {
            type = WalletType.valueOf(walletType.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.error("Invalid wallet type provided for admin.");
            throw new IllegalArgumentException("Unsupported wallet type.");
        }

        try {
            if (type == WalletType.PREPAID) {
                return adminPrepaidAmountRepository.findByAdminId_Id(adminId)
                        .orElseThrow(() -> {
                            log.warn("Admin prepaid wallet not found.");
                            return new RuntimeException("Admin prepaid wallet unavailable.");
                        });
            } else if (type == WalletType.POSTPAID) {
                return adminPostpaidAmountRepository.findByAdminId_Id(adminId)
                        .orElseThrow(() -> {
                            log.warn("Admin postpaid wallet not found.");
                            return new RuntimeException("Admin postpaid wallet unavailable.");
                        });
            } else {
                log.error("Unhandled wallet type for admin.");
                throw new IllegalArgumentException("Invalid wallet type for admin.");
            }
        } catch (Exception e) {
            log.error("Error retrieving admin wallet.", e);
            throw new RuntimeException("Failed to retrieve admin wallet.");
        }
    }

    public <T extends AbstractWalletAmountTable> void updateWallet(
            T wallet,
            String hierarchyId,
            WalletType walletType,
            BigDecimal amount,
            String tranxType,
            String baseTransactionId,
            String tranStatus,
            String service) {

        log.debug("Starting wallet update process for transaction type: {}, wallet type: {}", tranxType, walletType);

        String actionOnBalance;
        BigDecimal balanceBefore = wallet.getAvailableBalance();

        try {
            if ("credit".equalsIgnoreCase(tranxType)) {
                wallet.setTotalCash(wallet.getTotalCash().add(amount));
                wallet.setAvailableBalance(wallet.getAvailableBalance().add(amount));
                actionOnBalance = "Add Funds - " + walletType.name();
                log.info("Credited amount to wallet.");
            } else if ("debit".equalsIgnoreCase(tranxType)) {
                wallet.setTotalCash(wallet.getTotalCash().subtract(amount));
                wallet.setAvailableBalance(wallet.getAvailableBalance().subtract(amount));
                actionOnBalance = "Deduct Funds - " + walletType.name();
                log.info("Debited amount from wallet.");
            } else {
                log.warn("Invalid transaction type provided: {}", tranxType);
                throw new IllegalArgumentException("Unsupported transaction type.");
            }

            wallet.setLastUpdatedAmount(amount);
            wallet.setLastUpdatedDate(LocalDate.now());
            wallet.setLastUpdatedTime(LocalTime.now());

            log.debug("Wallet balances updated. Inserting transaction record.");

            insertTransaction(
                    walletType.name(),
                    hierarchyId,
                    tranStatus,
                    amount,
                    balanceBefore,
                    wallet.getAvailableBalance(),
                    actionOnBalance,
                    "TXN" + baseTransactionId,
                    service
            );

            log.info("Transaction record inserted successfully.");

        } catch (Exception e) {
            log.error("Error during wallet update operation.", e);
            throw new RuntimeException("Failed to update wallet.");
        }
    }


    private void insertTransaction(
            String walletType,
            String hierarchyId,
            String tranStatus,
            BigDecimal amount,
            BigDecimal beforeBalance,
            BigDecimal afterBalance,
            String actionOnBalanceAmount,
            String tranxId,
            String service) {

        log.debug("Preparing to insert transaction record.");

        try {
            String hierarchyType = extractHierarchyType(hierarchyId);
            String transactionTableName = getTransactionTable(hierarchyType, walletType);
            String columnName = getHierarchyColumnName(hierarchyType);

            log.info("Transaction details: Type={}, Amount={}, Status={}, Service={}",
                    service, amount, tranStatus, walletType);

            String sql = "INSERT INTO " + transactionTableName +
                    " (" + columnName + ", service, amount, date_of_transaction, time_of_transaction, " +
                    "bal_before_tran, bal_after_tran, action_on_balance_amount, final_balance, transaction_id, tran_status) " +
                    "VALUES (:hierarchyId, :service, :amount, :dateOfTransaction, :timeOfTransaction, " +
                    ":beforeTransactionBalance, :afterTransactionBalance, :actionOnBalanceAmount, :finalBalance, :tranxId, :tranStatus)";

            Query query = entityManager.createNativeQuery(sql)
                    .setParameter("hierarchyId", hierarchyId)
                    .setParameter("service", service)
                    .setParameter("amount", amount)
                    .setParameter("dateOfTransaction", LocalDate.now())
                    .setParameter("timeOfTransaction", LocalTime.now())
                    .setParameter("beforeTransactionBalance", beforeBalance)
                    .setParameter("afterTransactionBalance", afterBalance)
                    .setParameter("actionOnBalanceAmount", actionOnBalanceAmount)
                    .setParameter("finalBalance", afterBalance)
                    .setParameter("tranxId", tranxId)
                    .setParameter("tranStatus", tranStatus);

            query.executeUpdate();
            log.info("Transaction inserted successfully for ID: {}", tranxId);

        } catch (Exception e) {
            log.error("Failed to insert transaction record.", e);
            throw new RuntimeException("Transaction insert failed", e);
        }
    }

    public static Vendors getVendors(VendorRouting vendorRouting) {
        if (vendorRouting == null) {
            log.warn("Vendor routing input is null. Cannot proceed with vendor selection.");
            throw new IllegalArgumentException("Invalid vendor routing configuration.");
        }

        log.debug("Evaluating vendor routing priorities for active vendor selection.");

        if (vendorRouting.getRoutingPriority1() != null && vendorRouting.getRoutingPriority1().getActive()) {
            log.info("Selected vendor from priority 1.");
            return vendorRouting.getRoutingPriority1();
        } else if (vendorRouting.getRoutingPriority2() != null && vendorRouting.getRoutingPriority2().getActive()) {
            log.info("Selected vendor from priority 2.");
            return vendorRouting.getRoutingPriority2();
        } else if (vendorRouting.getRoutingPriority3() != null && vendorRouting.getRoutingPriority3().getActive()) {
            log.info("Selected vendor from priority 3.");
            return vendorRouting.getRoutingPriority3();
        }

        log.error("No active vendor found in any priority slot.");
        throw new RuntimeException("No active vendor available for the requested configuration.");
    }

    public BigDecimal calculateCommissionAmount(double rate, BigDecimal transactionAmount, String rateType) {
        log.debug("Calculating commission. RateType: {}, Rate: {}", rateType, rate);

        BigDecimal commissionRate = BigDecimal.valueOf(rate);
        BigDecimal commission;

        if ("PERCENTAGE".equalsIgnoreCase(rateType)) {
            commission = transactionAmount.multiply(commissionRate);
            log.debug("Commission calculated as percentage.");
        } else {
            commission = commissionRate;
            log.debug("Commission calculated as fixed amount.");
        }

        if (commission.compareTo(transactionAmount) > 0) {
            log.warn("Commission exceeds transaction amount. Capping to transaction amount.");
            commission = transactionAmount;
        }

        BigDecimal finalCommission = commission.setScale(2, RoundingMode.HALF_UP);
        log.info("Final commission amount: {}", finalCommission);
        return finalCommission;
    }

    public double getCommissionRateByType(String hierarchyType, SchemeRate schemeRate) {
        log.debug("Fetching commission rate for hierarchy type: {}", hierarchyType);

        double rate = switch (hierarchyType) {
            case "AD" -> schemeRate.getIm();
            case "ET" -> schemeRate.getEnterprise();
            case "WL" -> schemeRate.getWhitelabel();
            case "SD" -> schemeRate.getSuperDistributor();
            case "PT" -> schemeRate.getPartner();
            case "DT" -> schemeRate.getDistributor();
            case "RT" -> schemeRate.getRetailer();
            default -> {
                log.warn("Unknown hierarchy type: {}", hierarchyType);
                yield 0.0;
            }
        };

        log.info("Commission rate resolved: {}", rate);
        return rate;
    }

    public Optional<String> getAdminHierarchyId(String startHierarchyId) {
        log.debug("Searching for Admin hierarchy starting from: {}", startHierarchyId);

        String currentId = startHierarchyId;

        while (currentId != null && !currentId.isEmpty()) {
            String type = extractHierarchyType(currentId);
            log.trace("Current hierarchy type: {}", type);

            if ("AD".equals(type)) {
                log.info("Admin hierarchy found.");
                return Optional.of(currentId);
            }

            Optional<String> parentOpt = findParentHierarchy(currentId);
            if (parentOpt.isPresent()) {
                currentId = parentOpt.get();
            } else {
                log.info("No further parent found. Admin hierarchy not reachable.");
                break;
            }
        }

        return Optional.empty();
    }

    public <T extends AbstractWalletAmountTable> void performWalletUpdate(
            T wallet, String hierarchyId, WalletType walletType, BigDecimal amount,
            TransactionType type, String baseTxnId, String service, String remarks) {

        BigDecimal beforeBalance = wallet.getAvailableBalance();
        BigDecimal afterBalance;

        if (type == TransactionType.CREDIT) {
            wallet.setTotalCash(wallet.getTotalCash().add(amount));
            wallet.setAvailableBalance(beforeBalance.add(amount));
        } else {
            wallet.setTotalCash(wallet.getTotalCash().subtract(amount));
            wallet.setAvailableBalance(beforeBalance.subtract(amount));
        }

        wallet.setLastUpdatedAmount(amount);
        wallet.setLastUpdatedDate(LocalDate.now());
        wallet.setLastUpdatedTime(LocalTime.now());

        String action = (type == TransactionType.CREDIT ? "Add" : "Deduct") + " Funds - " + walletType.name();

        insertTransaction(
                walletType.name(), hierarchyId, Status.SUCCESS.name(), amount,
                beforeBalance, wallet.getAvailableBalance(), action,
                baseTxnId, type.name()
        );
    }

}