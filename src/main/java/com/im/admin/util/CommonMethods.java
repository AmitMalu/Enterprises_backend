package com.im.admin.util;

import com.im.admin.controller.AESController;
import com.im.admin.dto.*;
import com.im.admin.entities.*;
import com.im.admin.entities.enums.Status;
import com.im.admin.entities.enums.TransactionType;
import com.im.admin.entities.enums.WalletType;
import com.im.admin.entities.vendor.Vendors;
import com.im.admin.repositories.*;
import jakarta.persistence.*;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

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

    @Autowired
    private AdminUserRepository adminUserRepository;

    @Autowired
    private AESController aesController;

    @Autowired
    private AccessHierarchyNodeRepository accessHierarchyNodeRepository;

    @Autowired
    private WhiteLabelUserRepository whiteLabelUserRepository;

    RestTemplate restTemplate = new RestTemplate();


    public String generateNextIdForTables(String tableName, String idColumn) {
        log.debug("Generating next ID for table.");

        try {
//            String query = "SELECT MAX(" + idColumn + ") FROM " + tableName;
            String query = "SELECT MAX(CAST(" + idColumn + " AS UNSIGNED)) FROM " + tableName;
            log.trace("Executing query to fetch max ID."); // Avoid logging actual query in production

            String maxId = jdbcTemplate.queryForObject(query, String.class);

            if (maxId == null) {
                log.info("No existing ID found. Starting from default.");
                return "01";
            }

            int nextId = Integer.parseInt(maxId) + 1;

//            if (nextId > 99) {
//                log.error("ID generation failed: limit exceeded.");
//                throw new IllegalStateException("ID limit reached.");
//            }

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

    public BigDecimal getCurrentBalance(
            String hierarchyId,
            String walletType
    ) {

        log.debug(
                "Initiating balance retrieval | hierarchyId={} | walletType={}",
                hierarchyId,
                walletType
        );

        try {

            if (hierarchyId == null || hierarchyId.isBlank()) {
                throw new IllegalArgumentException(
                        "Hierarchy ID is required."
                );
            }

            if (walletType == null || walletType.isBlank()) {
                throw new IllegalArgumentException(
                        "Wallet type is required."
                );
            }

            String normalizedHierarchyId = hierarchyId.trim();
            String normalizedWalletType = walletType.trim().toUpperCase();

            String hierarchyType =
                    extractHierarchyType(normalizedHierarchyId);

            String resolvedHierarchyId = normalizedHierarchyId;
            String resolvedHierarchyType = hierarchyType;

            /*
             * User-level hierarchies do not have their own wallet.
             * Their parent hierarchy wallet should be used.
             */
            switch (hierarchyType.toUpperCase()) {

                case "RU" -> {

                    log.debug(
                            "Hierarchy is RetailerUser. Resolving parent Retailer | retailerUserId={}",
                            normalizedHierarchyId
                    );

                    RetailerUser retailerUser =
                            retailerUserRepository
                                    .findByIdWithRetailer(normalizedHierarchyId)
                                    .orElseThrow(() ->
                                            new RuntimeException(
                                                    "RetailerUser not found with ID: "
                                                            + normalizedHierarchyId
                                            )
                                    );

                    if (retailerUser.getRetailer() == null ||
                            retailerUser.getRetailer().getRetailerId() == null ||
                            retailerUser.getRetailer().getRetailerId().isBlank()) {

                        throw new RuntimeException(
                                "Parent Retailer is not mapped for RetailerUser: "
                                        + normalizedHierarchyId
                        );
                    }

                    resolvedHierarchyId =
                            retailerUser.getRetailer().getRetailerId();

                    resolvedHierarchyType = "RT";

                    log.debug(
                            "RetailerUser resolved successfully | retailerUserId={} | retailerId={}",
                            normalizedHierarchyId,
                            resolvedHierarchyId
                    );
                }

                case "AU" -> {

                    log.debug(
                            "Hierarchy is AdminUser. Resolving parent Admin | adminUserId={}",
                            normalizedHierarchyId
                    );

                    AdminUser adminUser =
                            adminUserRepository
                                    .findById(normalizedHierarchyId)
                                    .orElseThrow(() ->
                                            new RuntimeException(
                                                    "AdminUser not found with ID: "
                                                            + normalizedHierarchyId
                                            )
                                    );

                    if (adminUser.getParentAccount() == null ||
                            adminUser.getParentAccount().isBlank()) {

                        throw new RuntimeException(
                                "Parent Admin is not mapped for AdminUser: "
                                        + normalizedHierarchyId
                        );
                    }

                    resolvedHierarchyId =
                            adminUser.getParentAccount().trim();

                    resolvedHierarchyType = "AD";

                    log.debug(
                            "AdminUser resolved successfully | adminUserId={} | adminId={}",
                            normalizedHierarchyId,
                            resolvedHierarchyId
                    );
                }

                case "WLAU" -> {

                    log.debug(
                            "Hierarchy is WhiteLabelUser. Resolving parent WhiteLabel | whiteLabelUserId={}",
                            normalizedHierarchyId
                    );

                    WhiteLabelUser whiteLabelUser =
                            whiteLabelUserRepository
                                    .findById(normalizedHierarchyId)
                                    .orElseThrow(() ->
                                            new RuntimeException(
                                                    "WhiteLabelUser not found with ID: "
                                                            + normalizedHierarchyId
                                            )
                                    );

                    if (whiteLabelUser.getParentAccount() == null ||
                            whiteLabelUser.getParentAccount().isBlank()) {

                        throw new RuntimeException(
                                "Parent WhiteLabel is not mapped for WhiteLabelUser: "
                                        + normalizedHierarchyId
                        );
                    }

                    resolvedHierarchyId =
                            whiteLabelUser.getParentAccount().trim();

                    resolvedHierarchyType = "WL";

                    log.debug(
                            "WhiteLabelUser resolved successfully | whiteLabelUserId={} | whiteLabelId={}",
                            normalizedHierarchyId,
                            resolvedHierarchyId
                    );
                }

                default -> log.debug(
                        "Direct wallet hierarchy detected | hierarchyId={} | hierarchyType={}",
                        normalizedHierarchyId,
                        hierarchyType
                );
            }

            String amountTable =
                    getAmountTable(
                            resolvedHierarchyType,
                            normalizedWalletType
                    );

            String columnName =
                    getHierarchyColumnName(resolvedHierarchyType);

            if (amountTable == null || amountTable.isBlank()) {
                throw new IllegalStateException(
                        "Wallet amount table is not configured for hierarchy type: "
                                + resolvedHierarchyType
                                + " and wallet type: "
                                + normalizedWalletType
                );
            }

            if (columnName == null || columnName.isBlank()) {
                throw new IllegalStateException(
                        "Hierarchy column is not configured for hierarchy type: "
                                + resolvedHierarchyType
                );
            }

            /*
             * amountTable and columnName must only come from trusted internal
             * mappings. Never accept table or column names directly from request.
             */
            String sql =
                    "SELECT available_balance " +
                            "FROM " + amountTable + " " +
                            "WHERE " + columnName + " = :hierarchyId";

            log.debug(
                    "Executing balance lookup | originalHierarchyId={} | " +
                            "resolvedHierarchyId={} | resolvedHierarchyType={} | " +
                            "walletType={} | amountTable={}",
                    normalizedHierarchyId,
                    resolvedHierarchyId,
                    resolvedHierarchyType,
                    normalizedWalletType,
                    amountTable
            );

            Object result =
                    entityManager
                            .createNativeQuery(sql)
                            .setParameter(
                                    "hierarchyId",
                                    resolvedHierarchyId
                            )
                            .getSingleResult();

            BigDecimal balance;

            if (result == null) {
                balance = BigDecimal.ZERO;
            } else if (result instanceof BigDecimal bigDecimal) {
                balance = bigDecimal;
            } else if (result instanceof Number number) {
                balance = new BigDecimal(number.toString());
            } else {
                balance = new BigDecimal(result.toString());
            }

            log.info(
                    "Balance retrieved successfully | hierarchyId={} | " +
                            "resolvedHierarchyId={} | hierarchyType={} | walletType={} | balance={}",
                    normalizedHierarchyId,
                    resolvedHierarchyId,
                    resolvedHierarchyType,
                    normalizedWalletType,
                    balance
            );

            return balance;

        } catch (NoResultException e) {

            log.warn(
                    "No wallet balance record found | hierarchyId={} | walletType={}",
                    hierarchyId,
                    walletType
            );

            return BigDecimal.ZERO;

        } catch (IllegalArgumentException e) {

            log.warn(
                    "Invalid balance request | hierarchyId={} | walletType={} | reason={}",
                    hierarchyId,
                    walletType,
                    e.getMessage()
            );

            throw e;

        } catch (Exception e) {

            log.error(
                    "Error while retrieving wallet balance | hierarchyId={} | walletType={}",
                    hierarchyId,
                    walletType,
                    e
            );

            throw new RuntimeException(
                    "Unable to fetch current balance: " + e.getMessage(),
                    e
            );
        }
    }

    public BigDecimal getLeanAmount(
            String hierarchyId,
            String walletType
    ) {

        log.debug(
                "[LEAN AMOUNT METHOD] Initiating lean amount retrieval | hierarchyId={} | walletType={}",
                hierarchyId,
                walletType
        );

        try {

            if (hierarchyId == null || hierarchyId.isBlank()) {
                throw new IllegalArgumentException(
                        "Hierarchy ID is required."
                );
            }

            if (walletType == null || walletType.isBlank()) {
                throw new IllegalArgumentException(
                        "Wallet type is required."
                );
            }

            String normalizedHierarchyId =
                    hierarchyId.trim();

            String normalizedWalletType =
                    walletType.trim().toUpperCase();

            String hierarchyType =
                    extractHierarchyType(normalizedHierarchyId);

            if (hierarchyType == null || hierarchyType.isBlank()) {
                throw new IllegalArgumentException(
                        "Unable to determine hierarchy type for: "
                                + normalizedHierarchyId
                );
            }

            String resolvedHierarchyId =
                    normalizedHierarchyId;

            String resolvedHierarchyType =
                    hierarchyType.trim().toUpperCase();

            /*
             * User hierarchies do not own separate wallets.
             * Their parent hierarchy wallet must be used.
             */
            switch (resolvedHierarchyType) {

                case "RU" -> {

                    log.debug(
                            "[LEAN AMOUNT METHOD] Resolving RetailerUser parent | retailerUserId={}",
                            normalizedHierarchyId
                    );

                    RetailerUser retailerUser =
                            retailerUserRepository
                                    .findByIdWithRetailer(normalizedHierarchyId)
                                    .orElseThrow(() ->
                                            new RuntimeException(
                                                    "RetailerUser not found with ID: "
                                                            + normalizedHierarchyId
                                            )
                                    );

                    if (retailerUser.getRetailer() == null) {
                        throw new RuntimeException(
                                "Retailer is not mapped with RetailerUser: "
                                        + normalizedHierarchyId
                        );
                    }

                    String retailerId =
                            retailerUser.getRetailer().getRetailerId();

                    if (retailerId == null || retailerId.isBlank()) {
                        throw new RuntimeException(
                                "Parent Retailer ID is missing for RetailerUser: "
                                        + normalizedHierarchyId
                        );
                    }

                    resolvedHierarchyId =
                            retailerId.trim();

                    resolvedHierarchyType = "RT";

                    log.debug(
                            "[LEAN AMOUNT METHOD] RetailerUser resolved | retailerUserId={} | retailerId={}",
                            normalizedHierarchyId,
                            resolvedHierarchyId
                    );
                }

                case "AU" -> {

                    log.debug(
                            "[LEAN AMOUNT METHOD] Resolving AdminUser parent | adminUserId={}",
                            normalizedHierarchyId
                    );

                    AdminUser adminUser =
                            adminUserRepository
                                    .findById(normalizedHierarchyId)
                                    .orElseThrow(() ->
                                            new RuntimeException(
                                                    "AdminUser not found with ID: "
                                                            + normalizedHierarchyId
                                            )
                                    );

                    String parentAdminId =
                            adminUser.getParentAccount();

                    if (parentAdminId == null || parentAdminId.isBlank()) {
                        throw new RuntimeException(
                                "Parent Admin ID is missing for AdminUser: "
                                        + normalizedHierarchyId
                        );
                    }

                    resolvedHierarchyId =
                            parentAdminId.trim();

                    resolvedHierarchyType = "AD";

                    log.debug(
                            "[LEAN AMOUNT METHOD] AdminUser resolved | adminUserId={} | adminId={}",
                            normalizedHierarchyId,
                            resolvedHierarchyId
                    );
                }

                case "WLAU" -> {

                    log.debug(
                            "[LEAN AMOUNT METHOD] Resolving WhiteLabelUser parent | whiteLabelUserId={}",
                            normalizedHierarchyId
                    );

                    WhiteLabelUser whiteLabelUser =
                            whiteLabelUserRepository
                                    .findById(normalizedHierarchyId)
                                    .orElseThrow(() ->
                                            new RuntimeException(
                                                    "WhiteLabelUser not found with ID: "
                                                            + normalizedHierarchyId
                                            )
                                    );

                    String parentWhiteLabelId =
                            whiteLabelUser.getParentAccount();

                    if (parentWhiteLabelId == null ||
                            parentWhiteLabelId.isBlank()) {

                        throw new RuntimeException(
                                "Parent WhiteLabel ID is missing for WhiteLabelUser: "
                                        + normalizedHierarchyId
                        );
                    }

                    resolvedHierarchyId =
                            parentWhiteLabelId.trim();

                    resolvedHierarchyType = "WL";

                    log.debug(
                            "[LEAN AMOUNT METHOD] WhiteLabelUser resolved | whiteLabelUserId={} | whiteLabelId={}",
                            normalizedHierarchyId,
                            resolvedHierarchyId
                    );
                }

                default -> log.debug(
                        "[LEAN AMOUNT METHOD] Direct hierarchy wallet used | hierarchyId={} | hierarchyType={}",
                        normalizedHierarchyId,
                        resolvedHierarchyType
                );
            }

            String amountTable =
                    getAmountTable(
                            resolvedHierarchyType,
                            normalizedWalletType
                    );

            String columnName =
                    getHierarchyColumnName(
                            resolvedHierarchyType
                    );

            if (amountTable == null || amountTable.isBlank()) {
                throw new IllegalStateException(
                        "Amount table is not configured for hierarchy type: "
                                + resolvedHierarchyType
                                + " and wallet type: "
                                + normalizedWalletType
                );
            }

            if (columnName == null || columnName.isBlank()) {
                throw new IllegalStateException(
                        "Hierarchy column is not configured for hierarchy type: "
                                + resolvedHierarchyType
                );
            }

            /*
             * amountTable and columnName must come only from internal mappings.
             * Never take table or column names directly from request parameters.
             */
            String sql =
                    "SELECT cut_of_amount " +
                            "FROM " + amountTable + " " +
                            "WHERE " + columnName + " = :hierarchyId";

            log.debug(
                    "[LEAN AMOUNT METHOD] Executing lean amount query | " +
                            "originalHierarchyId={} | resolvedHierarchyId={} | " +
                            "resolvedHierarchyType={} | walletType={} | table={}",
                    normalizedHierarchyId,
                    resolvedHierarchyId,
                    resolvedHierarchyType,
                    normalizedWalletType,
                    amountTable
            );

            Object result =
                    entityManager
                            .createNativeQuery(sql)
                            .setParameter(
                                    "hierarchyId",
                                    resolvedHierarchyId
                            )
                            .getSingleResult();

            BigDecimal leanAmount;

            if (result == null) {

                leanAmount = BigDecimal.ZERO;

            } else if (result instanceof BigDecimal bigDecimal) {

                leanAmount = bigDecimal;

            } else if (result instanceof Number number) {

                leanAmount =
                        new BigDecimal(number.toString());

            } else {

                leanAmount =
                        new BigDecimal(result.toString());
            }

            log.info(
                    "[LEAN AMOUNT METHOD] Lean amount retrieved successfully | " +
                            "hierarchyId={} | resolvedHierarchyId={} | " +
                            "hierarchyType={} | walletType={} | leanAmount={}",
                    normalizedHierarchyId,
                    resolvedHierarchyId,
                    resolvedHierarchyType,
                    normalizedWalletType,
                    leanAmount
            );

            return leanAmount;

        } catch (NoResultException e) {

            log.warn(
                    "[LEAN AMOUNT METHOD] No lean amount record found | hierarchyId={} | walletType={}",
                    hierarchyId,
                    walletType
            );

            return BigDecimal.ZERO;

        } catch (IllegalArgumentException e) {

            log.warn(
                    "[LEAN AMOUNT METHOD] Invalid request | hierarchyId={} | walletType={} | reason={}",
                    hierarchyId,
                    walletType,
                    e.getMessage()
            );

            throw e;

        } catch (Exception e) {

            log.error(
                    "[LEAN AMOUNT METHOD] Error retrieving lean amount | hierarchyId={} | walletType={}",
                    hierarchyId,
                    walletType,
                    e
            );

            throw new RuntimeException(
                    "Unable to fetch lean amount: " + e.getMessage(),
                    e
            );
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
            case "AU" -> adminUserRepository.findById(hierarchyId);
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

//    public AbstractWalletAmountTable getWalletByType(String walletType, String hierarchyId) {
//
//        log.debug("Resolving wallet | hierarchyId={} | walletType={}",hierarchyId, walletType);
//
//        if (hierarchyId == null || hierarchyId.isBlank()) {
//            throw new IllegalArgumentException("Hierarchy ID is required for wallet resolution.");
//        }
//
//        if (walletType == null || walletType.isBlank()) {
//            throw new IllegalArgumentException("Wallet type is required for wallet resolution.");
//        }
//
//        String normalizedHierarchyId = hierarchyId.trim();
//
//        String normalizedWalletType = walletType.trim().toUpperCase();
//
//        String hierarchyType = extractHierarchyType(normalizedHierarchyId);
//
//        if (hierarchyType == null || hierarchyType.isBlank()) {
//            throw new IllegalArgumentException("Unable to identify hierarchy type for: " + normalizedHierarchyId);
//        }
//
//        hierarchyType = hierarchyType.trim().toUpperCase();
//
//        String resolvedHierarchyId = normalizedHierarchyId;
//
//        String resolvedHierarchyType = hierarchyType;
//
//        /*
//         * AU and WLAU are user accounts.
//         * They do not have separate wallets.
//         *
//         * AU   -> parent AD wallet
//         * WLAU -> parent WL wallet
//         */
//        switch (hierarchyType) {
//
//            case "AU" -> {
//
//                log.debug(
//                        "AdminUser detected. Resolving parent Admin wallet | adminUserId={}",
//                        normalizedHierarchyId
//                );
//
//                AdminUser adminUser =
//                        adminUserRepository
//                                .findById(normalizedHierarchyId)
//                                .orElseThrow(() ->
//                                        new IllegalArgumentException(
//                                                "AdminUser not found with ID: "
//                                                        + normalizedHierarchyId
//                                        )
//                                );
//
//                String parentAdminId = adminUser.getParentAccount();
//
//                if (parentAdminId == null || parentAdminId.isBlank()) {
//
//                    throw new IllegalStateException(
//                            "Parent Admin is not mapped for AdminUser: "
//                                    + normalizedHierarchyId
//                    );
//                }
//
//                resolvedHierarchyId = parentAdminId.trim();
//
//                resolvedHierarchyType = "AD";
//
//                log.debug(
//                        "AdminUser resolved to parent Admin wallet | adminUserId={} | adminId={}",
//                        normalizedHierarchyId, resolvedHierarchyId
//                );
//            }
//
//            case "WLAU" -> {
//
//                log.debug("WhiteLabelUser detected. Resolving parent WhiteLabel wallet | whiteLabelUserId={}", normalizedHierarchyId);
//
//                WhiteLabelUser whiteLabelUser =
//                        whiteLabelUserRepository
//                                .findById(normalizedHierarchyId)
//                                .orElseThrow(() ->
//                                        new IllegalArgumentException(
//                                                "WhiteLabelUser not found with ID: "
//                                                        + normalizedHierarchyId
//                                        )
//                                );
//
//                String parentWhiteLabelId = whiteLabelUser.getParentAccount();
//
//                if (parentWhiteLabelId == null || parentWhiteLabelId.isBlank()) {
//
//                    throw new IllegalStateException(
//                            "Parent WhiteLabel is not mapped for WhiteLabelUser: "
//                                    + normalizedHierarchyId
//                    );
//                }
//
//                resolvedHierarchyId = parentWhiteLabelId.trim();
//
//                resolvedHierarchyType = "WL";
//
//                log.debug(
//                        "WhiteLabelUser resolved to parent WhiteLabel wallet | " +
//                                "whiteLabelUserId={} | whiteLabelId={}",
//                        normalizedHierarchyId, resolvedHierarchyId
//                );
//            }
//
//            default -> log.debug(
//                    "Direct wallet hierarchy detected | hierarchyId={} | hierarchyType={}",
//                    normalizedHierarchyId,
//                    hierarchyType
//            );
//        }
//
//        log.debug(
//                "Final wallet resolution | originalHierarchyId={} | " +
//                        "originalHierarchyType={} | resolvedHierarchyId={} | " +
//                        "resolvedHierarchyType={} | walletType={}",
//                normalizedHierarchyId, hierarchyType, resolvedHierarchyId, resolvedHierarchyType, normalizedWalletType
//        );
//
//        AbstractWalletAmountTable wallet =
//                switch (resolvedHierarchyType) {
//
//                    case "RT" -> getRetailerWallet(normalizedWalletType, resolvedHierarchyId);
//
//                    case "DT" -> getDistributorWallet(normalizedWalletType, resolvedHierarchyId);
//
//                    case "PT" -> getPartnerWallet(normalizedWalletType, resolvedHierarchyId);
//
//                    case "SD" -> getSuperDistributorWallet(normalizedWalletType,resolvedHierarchyId);
//
//                    case "WL" -> getWhiteLabelWallet(normalizedWalletType, resolvedHierarchyId);
//
//                    case "ET" -> getEnterprisesWallet(normalizedWalletType, resolvedHierarchyId);
//
//                    case "AD", "IM" -> getAdminWallet(normalizedWalletType, resolvedHierarchyId);
//
//                    default -> {
//
//                        log.warn(
//                                "Unsupported hierarchy type for wallet resolution | " +
//                                        "hierarchyId={} | hierarchyType={}",
//                                resolvedHierarchyId, resolvedHierarchyType
//                        );
//
//                        throw new IllegalArgumentException("Unsupported hierarchy type for wallet resolution: " + resolvedHierarchyType);
//                    }
//                };
//
//        if (wallet == null) {
//
//            log.error(
//                    "Wallet resolution returned null | hierarchyId={} | hierarchyType={} | walletType={}",
//                    resolvedHierarchyId, resolvedHierarchyType, normalizedWalletType
//            );
//
//            throw new IllegalStateException(
//                    "Wallet is not configured for hierarchy ID: "
//                            + resolvedHierarchyId
//                            + ", hierarchy type: "
//                            + resolvedHierarchyType
//                            + ", wallet type: "
//                            + normalizedWalletType
//            );
//        }
//
//        log.info(
//                "Wallet resolved successfully | requestedHierarchyId={} | " +
//                        "resolvedHierarchyId={} | resolvedHierarchyType={} | walletType={}",
//                normalizedHierarchyId, resolvedHierarchyId, resolvedHierarchyType, normalizedWalletType
//        );
//
//        return wallet;
//    }

    public AbstractWalletAmountTable getWalletByType(
            String walletType,
            String hierarchyId
    ) {

        log.debug(
                "Resolving wallet | hierarchyId={} | walletType={}",
                hierarchyId,
                walletType
        );

        if (hierarchyId == null || hierarchyId.isBlank()) {
            throw new IllegalArgumentException(
                    "Hierarchy ID is required for wallet resolution."
            );
        }

        if (walletType == null || walletType.isBlank()) {
            throw new IllegalArgumentException(
                    "Wallet type is required for wallet resolution."
            );
        }

        String normalizedWalletType =
                walletType.trim().toUpperCase();

        /*
         * Important:
         *
         * AU   is resolved to its parent AD.
         * WLAU is resolved to its parent WL.
         *
         * Other hierarchy types remain unchanged.
         */
        ResolvedHierarchy resolvedHierarchy =
                resolveActualWalletHierarchy(hierarchyId);

        String resolvedHierarchyId =
                resolvedHierarchy.hierarchyId();

        String resolvedHierarchyType =
                resolvedHierarchy.hierarchyType();

        log.debug(
                "Final wallet resolution | requestedHierarchyId={} | resolvedHierarchyId={} | resolvedHierarchyType={} | walletType={}",
                hierarchyId,
                resolvedHierarchyId,
                resolvedHierarchyType,
                normalizedWalletType
        );

        AbstractWalletAmountTable wallet =
                switch (resolvedHierarchyType) {

                    case "RT" ->
                            getRetailerWallet(
                                    normalizedWalletType,
                                    resolvedHierarchyId
                            );

                    case "DT" ->
                            getDistributorWallet(
                                    normalizedWalletType,
                                    resolvedHierarchyId
                            );

                    case "PT" ->
                            getPartnerWallet(
                                    normalizedWalletType,
                                    resolvedHierarchyId
                            );

                    case "SD" ->
                            getSuperDistributorWallet(
                                    normalizedWalletType,
                                    resolvedHierarchyId
                            );

                    case "WL" ->
                            getWhiteLabelWallet(
                                    normalizedWalletType,
                                    resolvedHierarchyId
                            );

                    case "ET" ->
                            getEnterprisesWallet(
                                    normalizedWalletType,
                                    resolvedHierarchyId
                            );

                    case "AD", "IM" ->
                            getAdminWallet(
                                    normalizedWalletType,
                                    resolvedHierarchyId
                            );

                    default -> {

                        log.error(
                                "Unsupported hierarchy type for wallet resolution | hierarchyId={} | hierarchyType={} | walletType={}",
                                resolvedHierarchyId,
                                resolvedHierarchyType,
                                normalizedWalletType
                        );

                        throw new IllegalArgumentException(
                                "Unsupported hierarchy type for wallet resolution: "
                                        + resolvedHierarchyType
                        );
                    }
                };

        if (wallet == null) {

            log.error(
                    "Wallet resolution returned null | hierarchyId={} | hierarchyType={} | walletType={}",
                    resolvedHierarchyId,
                    resolvedHierarchyType,
                    normalizedWalletType
            );

            throw new IllegalStateException(
                    "Wallet is not configured for hierarchy ID: "
                            + resolvedHierarchyId
                            + ", hierarchy type: "
                            + resolvedHierarchyType
                            + ", wallet type: "
                            + normalizedWalletType
            );
        }

        log.info(
                "Wallet resolved successfully | requestedHierarchyId={} | resolvedHierarchyId={} | resolvedHierarchyType={} | walletType={}",
                hierarchyId,
                resolvedHierarchyId,
                resolvedHierarchyType,
                normalizedWalletType
        );

        return wallet;
    }

    public AbstractWalletAmountTable getRetailerWallet(String walletType, String retailerId) {
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
            String service,
            String subProductName,
            String narration,
            String remark,
            String schemeName,
            RetailersExtraColumnsDto retailersExtraColumnsDto,
            String vendorName,
            String operatorName) { //new


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
                    null,
                    tranStatus,
                    amount,
                    balanceBefore,
                    wallet.getAvailableBalance(),
                    actionOnBalance,
                    "TXN" + baseTransactionId,
                    service,
                    subProductName,
                    narration,
                    remark,
                    schemeName,
                    retailersExtraColumnsDto,
                    vendorName,
                    operatorName
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
            String relatedHierarchyId,
            String tranStatus,
            BigDecimal amount,
            BigDecimal beforeBalance,
            BigDecimal afterBalance,
            String actionOnBalanceAmount,
            String tranxId,
            String service,
            String subProductCode,
            String narration,
            String remark,
            String schemeName,
            RetailersExtraColumnsDto retailersExtraColumnsDto,
            String vendorName,
            String operatorName) {

        log.debug("Preparing to insert transaction record.");

        try {

            String hierarchyType = extractHierarchyType(hierarchyId);
            String transactionTableName = getTransactionTable(hierarchyType, walletType);
            String columnName = getHierarchyColumnName(hierarchyType);

            boolean isRetailer = "RT".equalsIgnoreCase(hierarchyType);

            // ===== CHECK IF ADMIN OR WHITE LABEL TABLE =====
            boolean isAdminTable = transactionTableName.contains("admin");
            boolean isWhiteLabelTable = transactionTableName.contains("white_label");
            boolean isSuperDistributor = transactionTableName.contains("super_distributor");
            boolean isDistributor = transactionTableName.contains("distributor");
            boolean isPartner = transactionTableName.contains("partner");


            // These tables don't have transaction_type, vendor_name, operator_name columns
            boolean excludeExtraColumns = isAdminTable || isWhiteLabelTable || isSuperDistributor || isPartner || isDistributor;

            String extraColumns = "";
            String extraValues = "";

            if (isRetailer) {
                extraColumns = ", request_domain_name, client_ip, medium";
                extraValues = ", :requestDomainName, :clientIp, :medium";
            }

            // ===== BUILD DYNAMIC COLUMNS =====
            String conditionalColumns = "";
            String conditionalValues = "";

            if (!excludeExtraColumns) {
                // Only add these columns for tables that support them (retailer tables)
                conditionalColumns = ", transaction_type, vendor_name, operator_name";
                conditionalValues = ", :transactionType, :vendorName, :operatorName";
            }

            boolean includeHierarchy = shouldInsertHierarchyColumn(hierarchyType);

            log.info("Transaction details: Type={}, Amount={}, Status={}, Service={}, Table={}",
                    service, amount, tranStatus, walletType, transactionTableName);

            // ===== DYNAMIC SQL QUERY =====
            String sql = "INSERT INTO " + transactionTableName +
                    " (" + columnName +
                    (includeHierarchy ? ", hierarchy_id" : "") +
                    ", service, amount, date_of_transaction, time_of_transaction, " +
                    "bal_before_tran, bal_after_tran, action_on_balance_amount, final_balance, transaction_id, tran_status," +
                    " sub_product_name, narration, remarks, scheme_name, latitude, longitude" +
                    conditionalColumns +  // Add conditional columns
                    extraColumns + ") " +
                    "VALUES (:" + columnName +
                    (includeHierarchy ? ", :relatedHierarchyId" : "") +
                    ", :service, :amount, :dateOfTransaction, :timeOfTransaction, " +
                    ":beforeTransactionBalance, :afterTransactionBalance, :actionOnBalanceAmount, :finalBalance, :tranxId," +
                    " :tranStatus, :subProductCode, :narration, :remark, :schemeName, :latitude, :longitude" +
                    conditionalValues +  // Add conditional values
                    extraValues + ")";

            Query query = entityManager.createNativeQuery(sql)
                    .setParameter(columnName, hierarchyId)
                    .setParameter("service", service)
                    .setParameter("amount", amount)
                    .setParameter("dateOfTransaction", LocalDate.now())
                    .setParameter("timeOfTransaction", LocalTime.now())
                    .setParameter("beforeTransactionBalance", beforeBalance)
                    .setParameter("afterTransactionBalance", afterBalance)
                    .setParameter("actionOnBalanceAmount", actionOnBalanceAmount)
                    .setParameter("finalBalance", afterBalance)
                    .setParameter("tranxId", tranxId)
                    .setParameter("tranStatus", tranStatus)
                    .setParameter("subProductCode", subProductCode)
                    .setParameter("narration", narration)
                    .setParameter("remark", remark)
                    .setParameter("schemeName", schemeName)
                    .setParameter("latitude", retailersExtraColumnsDto.getLatitude())
                    .setParameter("longitude", retailersExtraColumnsDto.getLongitude());

            // ===== SET CONDITIONAL PARAMETERS ONLY IF NOT EXCLUDED =====
            if (!excludeExtraColumns) {
                query.setParameter("transactionType", service)
                        .setParameter("vendorName", vendorName)
                        .setParameter("operatorName", operatorName);
            }

            if (includeHierarchy) {
                query.setParameter("relatedHierarchyId", relatedHierarchyId);
            }

            if (isRetailer) {
                query.setParameter("requestDomainName", retailersExtraColumnsDto.getRequestDomainName())
                        .setParameter("clientIp", retailersExtraColumnsDto.getClientIp())
                        .setParameter("medium", retailersExtraColumnsDto.getMedium());
            }

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
            //commissionRate = commissionRate
              //      .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP); // convert to fraction
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
            TransactionType type, String baseTxnId, String remarks, String subProductName, String narration,
            String relatedHierarchyId, RetailersExtraColumnsDto retailersExtraColumnsDto) {

        BigDecimal beforeBalance = wallet.getAvailableBalance();

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
                walletType.name(), hierarchyId, relatedHierarchyId, Status.SUCCESS.name(), amount,
                beforeBalance, wallet.getAvailableBalance(), action,
                baseTxnId, type.name(), subProductName, narration, remarks,"", retailersExtraColumnsDto,
                null, null
        );
    }

    private boolean shouldInsertHierarchyColumn(
            String hierarchyType
    ) {

        return "AD".equals(hierarchyType)
                || "WL".equals(hierarchyType);
    }

    public String buildTransactionId(String prefix, String baseId) {
        return prefix + baseId;
    }

    public String generateTokenForAES(VendorCredentials vendorCredentials) {
        // Call AES encrypt service
        ResponseEntity<?> responseEntity = aesController.encryptCredentials(
                new EncryptDecryptCredentialsDto(vendorCredentials.getUsername(), vendorCredentials.getPassword())
        );

        @SuppressWarnings("unchecked")
        Map<String, String> encryptedMap = (Map<String, String>) responseEntity.getBody();
        assert encryptedMap != null;
        String encUser = encryptedMap.get("cipherUsername");
        String encPass = encryptedMap.get("cipherPassword");
        String salt = encryptedMap.get("salt");
        String iv = encryptedMap.get("iv");

        // Build JSON for token request
        Map<String, String> jsonBody = new HashMap<>();
        jsonBody.put("username", encUser);
        jsonBody.put("password", encPass);
        jsonBody.put("client_id", vendorCredentials.getClientId());
        jsonBody.put("salt", salt);
        jsonBody.put("ivBase64", iv);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(jsonBody, headers);

        //String uatUrl = vendorCredentials.getTokenUrlUat();
        String prodUrl = vendorCredentials.getTokenUrlProd();

        ResponseEntity<Map> response = restTemplate.postForEntity(prodUrl, requestEntity, Map.class);
        Map responseBodyMap = response.getBody();
        if (responseBodyMap == null || responseBodyMap.get("token") == null) {
            throw new RuntimeException("AES token generation failed: " + responseBodyMap);
        }

        return (String) responseBodyMap.get("token");
    }

    public String formatOperatorName(String operator) {
        if (operator == null || operator.isEmpty()) {
            return operator;
        }

        // Fallback to generic formatting
        return Arrays.stream(operator.split("_"))
                .map(part -> part.substring(0, 1).toUpperCase() + part.substring(1).toLowerCase())
                .collect(Collectors.joining("_"));
    }

    public boolean isValidIpAddress(String ip) {
        if (ip == null || ip.trim().isEmpty()) {
            return false;
        }

        // IPv4 pattern (e.g., 1.22.36.186)
        String ipv4Pattern = "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$";
        return ip.trim().matches(ipv4Pattern);
    }

    public BigDecimal resolveTdsRateForHierarchy(
            String hierarchyId,
            String hierarchyType) {

        Boolean aadhaarPanVerified =
                switch (hierarchyType) {

                    case "ET" ->
                            enterprisesRepository.findById(hierarchyId)
                                    .map(Enterprises::getAadhaarPanVerified)
                                    .orElseThrow(() ->
                                            hierarchyNotFound(
                                                    hierarchyId,
                                                    hierarchyType
                                            )
                                    );

                    case "WL" ->
                            whiteLabelsRepository.findById(hierarchyId)
                                    .map(WhiteLabels::getAadhaarPanVerified)
                                    .orElseThrow(() ->
                                            hierarchyNotFound(
                                                    hierarchyId,
                                                    hierarchyType
                                            )
                                    );

                    case "SD" ->
                            superDistributorRepository.findById(hierarchyId)
                                    .map(SuperDistributor::getAadhaarPanVerified)
                                    .orElseThrow(() ->
                                            hierarchyNotFound(
                                                    hierarchyId,
                                                    hierarchyType
                                            )
                                    );

                    case "PT" ->
                            partnerRepository.findById(hierarchyId)
                                    .map(Partner::getAadhaarPanVerified)
                                    .orElseThrow(() ->
                                            hierarchyNotFound(
                                                    hierarchyId,
                                                    hierarchyType
                                            )
                                    );

                    case "DT" ->
                            distributorRepository.findById(hierarchyId)
                                    .map(Distributor::getAadhaarPanVerified)
                                    .orElseThrow(() ->
                                            hierarchyNotFound(
                                                    hierarchyId,
                                                    hierarchyType
                                            )
                                    );

                    case "RT" ->
                            retailerRepository.findById(hierarchyId)
                                    .map(Retailer::getAadhaarPanVerified)
                                    .orElseThrow(() ->
                                            hierarchyNotFound(
                                                    hierarchyId,
                                                    hierarchyType
                                            )
                                    );

                    /*
                     * Retailer user acts using the retailer account.
                     * Resolve its parent retailer and use the retailer's verification flag.
                     */
                    case "RU" -> {
                        RetailerUser retailerUser =
                                retailerUserRepository.findById(hierarchyId)
                                        .orElseThrow(() ->
                                                hierarchyNotFound(
                                                        hierarchyId,
                                                        hierarchyType
                                                )
                                        );

                        String retailerId =
                                trimToNull(
                                        retailerUser.getParentAccount()
                                );

                        if (retailerId == null) {
                            throw new IllegalStateException(
                                    "Parent retailer is missing for retailer user: "
                                            + hierarchyId
                            );
                        }

                        yield retailerRepository.findById(retailerId)
                                .map(Retailer::getAadhaarPanVerified)
                                .orElseThrow(() ->
                                        new IllegalStateException(
                                                "Parent retailer not found: "
                                                        + retailerId
                                                        + ", retailerUserId: "
                                                        + hierarchyId
                                        )
                                );
                    }

                    /*
                     * Admin and Admin User TDS are not calculated here.
                     */
                    case "AD", "AU" -> {
                        log.debug(
                                "[COMMISSION] Admin hierarchy does not require verification-based TDS | HierarchyId={} | Type={}",
                                hierarchyId,
                                hierarchyType
                        );

                        yield null;
                    }

                    default ->
                            throw new IllegalArgumentException(
                                    "Unsupported hierarchy type for TDS calculation: "
                                            + hierarchyType
                                            + ", hierarchyId: "
                                            + hierarchyId
                            );
                };

        if ("AD".equals(hierarchyType) || "AU".equals(hierarchyType)) {
            return ZERO_TDS_RATE;
        }

        return resolveTdsRate(
                aadhaarPanVerified,
                hierarchyId
        );
    }

    public BigDecimal resolveTdsRate(
            Boolean aadhaarPanVerified,
            String hierarchyId) {

        boolean verified =
                Boolean.TRUE.equals(aadhaarPanVerified);

        BigDecimal applicableTdsRate =
                verified
                        ? VERIFIED_TDS_RATE
                        : UNVERIFIED_TDS_RATE;

        log.info(
                "[COMMISSION] TDS rate resolved | HierarchyId={} | AadhaarPanVerified={} | TdsRate={}",
                hierarchyId,
                verified,
                applicableTdsRate
        );

        return applicableTdsRate;
    }

    private IllegalStateException hierarchyNotFound(
            String hierarchyId,
            String hierarchyType) {

        return new IllegalStateException(
                "Hierarchy entity not found while resolving TDS"
                        + " | hierarchyId=" + hierarchyId
                        + " | hierarchyType=" + hierarchyType
        );
    }

    public String trimToNull(String value) {

        if (value == null) {
            return null;
        }

        String trimmed =
                value.trim();

        return trimmed.isEmpty()
                ? null
                : trimmed;
    }

    private static final BigDecimal VERIFIED_TDS_RATE =
            new BigDecimal("2.00");

    private static final BigDecimal UNVERIFIED_TDS_RATE =
            new BigDecimal("20.00");

    private static final BigDecimal ZERO_TDS_RATE =
            BigDecimal.ZERO;

    public boolean hasFullSystemAccess(
            String loginHierarchyType
    ) {

        return "AD".equalsIgnoreCase(loginHierarchyType)
                || "AU".equalsIgnoreCase(loginHierarchyType);
    }

    public static boolean hasText(String value) {
        return value != null
                && !value.trim().isEmpty();
    }

    public String resolveEffectiveHierarchyId(
            String loginHierarchyId,
            String loginHierarchyType
    ) {

        if (loginHierarchyId == null
                || loginHierarchyId.isBlank()) {

            throw new IllegalArgumentException(
                    "Login hierarchy ID is required"
            );
        }

        if (loginHierarchyType == null
                || loginHierarchyType.isBlank()) {

            throw new IllegalArgumentException(
                    "Login hierarchy type is required"
            );
        }

        final String normalizedLoginHierarchyId =
                loginHierarchyId.trim();

        final String normalizedLoginType =
                loginHierarchyType
                        .trim()
                        .toUpperCase();

        final String expectedParentType;

        switch (normalizedLoginType) {

            case "AU" -> expectedParentType = "AD";

            case "WLAU" -> expectedParentType = "WL";

            default -> {
                return normalizedLoginHierarchyId;
            }
        }

        AccessHierarchyNode delegatedUserNode =
                accessHierarchyNodeRepository
                        .findById(normalizedLoginHierarchyId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Hierarchy node not found: "
                                                + normalizedLoginHierarchyId
                                )
                        );

        if (!normalizedLoginType.equalsIgnoreCase(
                delegatedUserNode.getHierarchyType()
        )) {

            throw new IllegalStateException(
                    "Hierarchy type mismatch. "
                            + "Hierarchy ID: "
                            + normalizedLoginHierarchyId
                            + ", expected type: "
                            + normalizedLoginType
                            + ", actual type: "
                            + delegatedUserNode.getHierarchyType()
            );
        }

        String rawParentHierarchyId =
                delegatedUserNode.getParentHierarchyId();

        if (rawParentHierarchyId == null
                || rawParentHierarchyId.isBlank()
                || "-".equals(rawParentHierarchyId.trim())) {

            throw new IllegalStateException(
                    "Parent hierarchy is not configured for: "
                            + normalizedLoginHierarchyId
            );
        }

        final String parentHierarchyId =
                rawParentHierarchyId.trim();

        AccessHierarchyNode parentNode =
                accessHierarchyNodeRepository
                        .findById(parentHierarchyId)
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "Parent hierarchy node not found. "
                                                + "Login hierarchy: "
                                                + normalizedLoginHierarchyId
                                                + ", parent hierarchy: "
                                                + parentHierarchyId
                                )
                        );

        if (!expectedParentType.equalsIgnoreCase(
                parentNode.getHierarchyType()
        )) {

            throw new IllegalStateException(
                    "Invalid delegated hierarchy relationship. "
                            + "Login hierarchy: "
                            + normalizedLoginHierarchyId
                            + ", login type: "
                            + normalizedLoginType
                            + ", parent hierarchy: "
                            + parentHierarchyId
                            + ", expected parent type: "
                            + expectedParentType
                            + ", actual parent type: "
                            + parentNode.getHierarchyType()
            );
        }

        log.info(
                "Effective hierarchy resolved | "
                        + "loginHierarchyId={} | "
                        + "loginHierarchyType={} | "
                        + "effectiveHierarchyId={} | "
                        + "effectiveHierarchyType={}",
                normalizedLoginHierarchyId,
                normalizedLoginType,
                parentHierarchyId,
                expectedParentType
        );

        return parentHierarchyId;
    }

    public String resolveWalletOwnerHierarchyId(String hierarchyId) {

        if (hierarchyId == null || hierarchyId.isBlank()) {
            throw new IllegalArgumentException(
                    "Hierarchy ID is required for wallet owner resolution."
            );
        }

        String normalizedHierarchyId = hierarchyId.trim();

        String hierarchyType =
                extractHierarchyType(normalizedHierarchyId);

        if (hierarchyType == null || hierarchyType.isBlank()) {
            throw new IllegalArgumentException(
                    "Unable to identify hierarchy type for: "
                            + normalizedHierarchyId
            );
        }

        return switch (hierarchyType.toUpperCase()) {

            case "AU" -> {

                AdminUser adminUser =
                        adminUserRepository
                                .findById(normalizedHierarchyId)
                                .orElseThrow(() ->
                                        new IllegalArgumentException(
                                                "AdminUser not found with ID: "
                                                        + normalizedHierarchyId
                                        )
                                );

                String parentAdminId =
                        adminUser.getParentAccount();

                if (parentAdminId == null ||
                        parentAdminId.isBlank()) {

                    throw new IllegalStateException(
                            "Parent Admin is not mapped for AdminUser: "
                                    + normalizedHierarchyId
                    );
                }

                log.debug(
                        "Resolved AdminUser to wallet owner | adminUserId={} | adminId={}",
                        normalizedHierarchyId,
                        parentAdminId
                );

                yield parentAdminId.trim();
            }

            case "WLAU" -> {

                WhiteLabelUser whiteLabelUser =
                        whiteLabelUserRepository
                                .findById(normalizedHierarchyId)
                                .orElseThrow(() ->
                                        new IllegalArgumentException(
                                                "WhiteLabelUser not found with ID: "
                                                        + normalizedHierarchyId
                                        )
                                );

                String parentWhiteLabelId =
                        whiteLabelUser.getParentAccount();

                if (parentWhiteLabelId == null ||
                        parentWhiteLabelId.isBlank()) {

                    throw new IllegalStateException(
                            "Parent WhiteLabel is not mapped for WhiteLabelUser: "
                                    + normalizedHierarchyId
                    );
                }

                log.debug(
                        "Resolved WhiteLabelUser to wallet owner | whiteLabelUserId={} | whiteLabelId={}",
                        normalizedHierarchyId,
                        parentWhiteLabelId
                );

                yield parentWhiteLabelId.trim();
            }

            case "RU" -> {

                RetailerUser retailerUser =
                        retailerUserRepository
                                .findByIdWithRetailer(normalizedHierarchyId)
                                .orElseThrow(() ->
                                        new IllegalArgumentException(
                                                "RetailerUser not found with ID: "
                                                        + normalizedHierarchyId
                                        )
                                );

                if (retailerUser.getRetailer() == null ||
                        retailerUser.getRetailer().getRetailerId() == null ||
                        retailerUser.getRetailer().getRetailerId().isBlank()) {

                    throw new IllegalStateException(
                            "Parent Retailer is not mapped for RetailerUser: "
                                    + normalizedHierarchyId
                    );
                }

                String retailerId =
                        retailerUser.getRetailer().getRetailerId().trim();

                log.debug(
                        "Resolved RetailerUser to wallet owner | retailerUserId={} | retailerId={}",
                        normalizedHierarchyId,
                        retailerId
                );

                yield retailerId;
            }

            default -> normalizedHierarchyId;
        };
    }

    public ResolvedHierarchy resolveActualWalletHierarchy(
            String hierarchyId
    ) {

        if (hierarchyId == null || hierarchyId.isBlank()) {
            throw new IllegalArgumentException(
                    "Hierarchy ID is required for hierarchy resolution."
            );
        }

        String normalizedHierarchyId =
                hierarchyId.trim();

        String hierarchyType =
                extractHierarchyType(normalizedHierarchyId);

        if (hierarchyType == null || hierarchyType.isBlank()) {

            log.error(
                    "Unable to extract hierarchy type | hierarchyId={}",
                    normalizedHierarchyId
            );

            throw new IllegalArgumentException(
                    "Unable to identify hierarchy type for: "
                            + normalizedHierarchyId
            );
        }

        String normalizedHierarchyType =
                hierarchyType.trim().toUpperCase();

        log.debug(
                "Resolving actual wallet hierarchy | hierarchyId={} | hierarchyType={}",
                normalizedHierarchyId,
                normalizedHierarchyType
        );

        switch (normalizedHierarchyType) {

            case "AU" -> {

                AdminUser adminUser =
                        adminUserRepository
                                .findById(normalizedHierarchyId)
                                .orElseThrow(() ->
                                        new IllegalArgumentException(
                                                "AdminUser not found with ID: "
                                                        + normalizedHierarchyId
                                        )
                                );

                String parentAdminId =
                        adminUser.getParentAccount();

                if (parentAdminId == null
                        || parentAdminId.isBlank()) {

                    log.error(
                            "Parent Admin is not mapped | adminUserId={}",
                            normalizedHierarchyId
                    );

                    throw new IllegalStateException(
                            "Parent Admin is not mapped for AdminUser: "
                                    + normalizedHierarchyId
                    );
                }

                String resolvedAdminId =
                        parentAdminId.trim();

                log.info(
                        "AdminUser resolved to parent Admin | adminUserId={} | adminId={}",
                        normalizedHierarchyId,
                        resolvedAdminId
                );

                return new ResolvedHierarchy(
                        resolvedAdminId,
                        "AD"
                );
            }

            case "WLAU" -> {

                WhiteLabelUser whiteLabelUser =
                        whiteLabelUserRepository
                                .findById(normalizedHierarchyId)
                                .orElseThrow(() ->
                                        new IllegalArgumentException(
                                                "WhiteLabelUser not found with ID: "
                                                        + normalizedHierarchyId
                                        )
                                );

                String parentWhiteLabelId =
                        whiteLabelUser.getParentAccount();

                if (parentWhiteLabelId == null
                        || parentWhiteLabelId.isBlank()) {

                    log.error(
                            "Parent WhiteLabel is not mapped | whiteLabelUserId={}",
                            normalizedHierarchyId
                    );

                    throw new IllegalStateException(
                            "Parent WhiteLabel is not mapped for WhiteLabelUser: "
                                    + normalizedHierarchyId
                    );
                }

                String resolvedWhiteLabelId =
                        parentWhiteLabelId.trim();

                log.info(
                        "WhiteLabelUser resolved to parent WhiteLabel | whiteLabelUserId={} | whiteLabelId={}",
                        normalizedHierarchyId,
                        resolvedWhiteLabelId
                );

                return new ResolvedHierarchy(
                        resolvedWhiteLabelId,
                        "WL"
                );
            }

            default -> {

                return new ResolvedHierarchy(
                        normalizedHierarchyId,
                        normalizedHierarchyType
                );
            }
        }
    }

    public <T> void validateDebitTransaction(
            String retailerId,
            WalletType walletType,
            BigDecimal availableBalance,
            BigDecimal txnAmount,
            BigDecimal grossAmount
    ) {

        BigDecimal leanAmount = getLeanAmount(retailerId, walletType.name());

        BigDecimal finalDeductAmount =
                txnAmount.add(grossAmount);

        // 1. Insufficient wallet balance
        if (availableBalance.compareTo(finalDeductAmount) < 0) {

            throw new IllegalArgumentException(
                    "Insufficient Balance In " + walletType
            );
        }

        // 2. Lean amount validation
        BigDecimal balanceAfterTxn =
                availableBalance.subtract(txnAmount);

        if (balanceAfterTxn.compareTo(leanAmount) < 0) {

            throw new IllegalArgumentException(
                    String.format(
                            "Transaction not allowed. ₹%s must remain as lien balance.",
                            leanAmount
                    )
            );
        }

        // 3. Lean + Gross validation
        BigDecimal balanceAfterGross =
                availableBalance.subtract(finalDeductAmount);

        if (balanceAfterGross.compareTo(leanAmount) < 0) {

            throw new IllegalArgumentException(
                    String.format(
                            "Insufficient usable balance after charges. ₹%s must remain locked as lien amount.",
                            leanAmount
                    )
            );
        }

    }

}