package com.scs.admin.service.impl;

import com.scs.admin.dto.AutoChargeMasterRequestDTO;
import com.scs.admin.entities.Admin;
import com.scs.admin.entities.ChargeMaster;
import com.scs.admin.entities.Retailer;
import com.scs.admin.entities.WhiteLabels;
import com.scs.admin.repositories.*;
import com.scs.admin.service.ChargeMasterService;
import com.scs.admin.util.CommonMethods;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChargeMasterServiceImpl implements ChargeMasterService {

    @Autowired
    private RetailerRepository retailerRepository;

    @Autowired
    private DistributorRepository distributorRepository;

    @Autowired
    private PartnerRepository partnerRepository;

    @Autowired
    private SuperDistributorRepository superDistributorRepository;

    @Autowired
    private WhiteLabelRepository whiteLabelsRepository;

    @Autowired
    private EnterprisesRepository enterprisesRepository;

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private ChargeMasterRepository repository;

    @Autowired
    private CommonMethods commonMethods;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public ChargeMaster createChargeMaster(ChargeMaster cm) {
        log.info("Creating ChargeMaster: {}", cm.getName());

        if (repository.existsByName(cm.getName())) {
            log.error("ChargeMaster creation failed. Name already exists: {}", cm.getName());
            throw new RuntimeException("ChargeMaster with this name already exists");
        }

        double totalAmount = calculateTotalAmount(cm.getAmount(), cm.getGst());
        log.info("Calculated total amount: {} (Amount={}, GST={})", totalAmount, cm.getAmount(), cm.getGst());
        cm.setTotalAmount(totalAmount);

        ChargeMaster saved = repository.save(cm);
        log.info("ChargeMaster created successfully: ID={}, Name={}", saved.getId(), saved.getName());
        return saved;
    }

    @Override
    public ChargeMaster updateChargeMaster(Long id, ChargeMaster cm) {
        log.info("Updating ChargeMaster ID={}", id);

        ChargeMaster existing = repository.findById(id)
                .orElseThrow(() -> {
                    log.error("ChargeMaster not found with ID={}", id);
                    return new RuntimeException("ChargeMaster not found");
                });

        existing.setName(cm.getName());
        existing.setInterval(cm.getInterval());
        existing.setAmount(cm.getAmount());
        existing.setGst(cm.getGst());

        double totalAmount = calculateTotalAmount(cm.getAmount(), cm.getGst());
        log.info("Updated total amount: {} (Amount={}, GST={})", totalAmount, cm.getAmount(), cm.getGst());
        existing.setTotalAmount(totalAmount);

        ChargeMaster updated = repository.save(existing);
        log.info("ChargeMaster updated successfully: ID={}, Name={}", updated.getId(), updated.getName());
        return updated;
    }


    @Override
    public void deleteChargeMaster(Long id) {
        log.info("Deleting ChargeMaster ID: {}", id);
        repository.deleteById(id);
    }

    @Override
    public ChargeMaster getChargeMaster(Long id) {
        log.info("Fetching ChargeMaster ID: {}", id);
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("ChargeMaster not found"));
    }

    @Override
    public List<ChargeMaster> getAllChargeMasters() {
        log.info("Fetching all ChargeMasters");
        return repository.findAll();
    }

    @Override
    public double calculateTotalAmount(Double amount, Double gst) {
        return amount + (amount * gst / 100);
    }

    @Transactional
    public String applyAutoCharge(AutoChargeMasterRequestDTO request) {
        // 1. Fetch retailer
        Retailer retailer = retailerRepository.findByRetailerId(request.getRetailerId())
                .orElseThrow(() -> new RuntimeException("Retailer not found"));

        // 2. Fetch charge master
        ChargeMaster charge = repository.findById(request.getChargeMasterId())
                .orElseThrow(() -> new RuntimeException("ChargeMaster not found"));

        // 3. Generate transaction ID
        String baseTxnId = commonMethods.generateBaseTransactionId();
        Double totalAmount = charge.getTotalAmount();
        String walletType = request.getWalletType();

        // 4. Retailer DEBIT transaction
        insertTransaction(
                "RT", retailer.getRetailerId(), walletType, totalAmount, "CHG" + baseTxnId,
                "AUTO_CHARGE: " + charge.getName(), false
        );

        // 5. Update retailer wallet (debit)
        updateWalletAmount("retailer", retailer.getRetailerId(), walletType, totalAmount);

        String currentId = retailer.getRetailerId();

        while (true) {
            Optional<String> parentOpt = findParentHierarchy(currentId);
            if (parentOpt.isEmpty()) break;

            String parentId = parentOpt.get();
            String parentType = commonMethods.extractHierarchyType(parentId);

            if ("WL".equals(parentType)) {
                Optional<WhiteLabels> wlOpt = whiteLabelsRepository.findById(parentId);
                if (wlOpt.isPresent() && Boolean.TRUE.equals(wlOpt.get().getIsAdmin())) {
                    //insert transaction
                    insertTransaction(
                            "WL", parentId, walletType, totalAmount, "CHG" + baseTxnId,
                            "AUTO_CHARGE: " + charge.getName(), true
                    );
                    updateWalletAmount("white_label", parentId, walletType, totalAmount);
                    return "Auto charge applied. Credited to Admin WhiteLabel: " + parentId;
                }
            }

            if ("SCS".equals(parentType) || "AD".equals(parentType)) {
                //insert transaction
                insertTransaction(
                        "AD", parentId, walletType, totalAmount, "CHG" + baseTxnId,
                        "AUTO_CHARGE: " + charge.getName(), true
                );
                updateWalletAmount("admin", parentId, walletType, totalAmount);
                return "Auto charge applied. Credited to Admin: " + parentId;
            }

            currentId = parentId;
        }

        return "Auto charge applied successfully.";
    }


    public void insertTransaction(String hierarchyType, String id, String walletType,
                                  Double amount, String txnId, String remarks, boolean isCredit) {

        String tableName = commonMethods.getTransactionTable(hierarchyType, walletType);
        String txnType = isCredit ? "CREDIT" : "DEBIT";

        // Get current balance
        BigDecimal balBefore = getCurrentAvailableBalance(hierarchyType, id, walletType);
        BigDecimal txnAmount = BigDecimal.valueOf(amount);

        // Calculate balances
        BigDecimal balAfter = isCredit ? balBefore.add(txnAmount) : balBefore.subtract(txnAmount);
        BigDecimal finalBalance = balAfter; // assuming final = after for this txn type

        jdbcTemplate.update(
                "INSERT INTO " + tableName + " (id, transaction_id, amount, type, remarks, created_on, " +
                        "bal_before_tran, bal_after_tran, final_balance, tran_status, action_on_balance_amount, service) " +
                        "VALUES (?, ?, ?, ?, ?, NOW(), ?, ?, ?, ?, ?, ?)",
                id,
                txnId,
                txnAmount,
                txnType,
                remarks,
                balBefore,
                balAfter,
                finalBalance,
                "SUCCESS", // or dynamic
                isCredit ? "CREDIT" : "DEBIT",
                "Charge"  // or dynamic, for now assuming Charge
        );
    }

    public void updateWalletAmount(String hierarchyType, String id, String walletType, Double delta) {
        String tableName = commonMethods.getAmountTable(hierarchyType, walletType);

        BigDecimal change = BigDecimal.valueOf(delta);
        BigDecimal lastAmount = change.abs();

        jdbcTemplate.update(
                "UPDATE " + tableName + " SET " +
                        "used_cash = used_cash + ?, " +
                        "available_balance = available_balance + ?, " +
                        "last_updated_amount = ?, " +
                        "last_updated_date = NOW(), " +
                        "last_updated_time = NOW() " +
                        "WHERE id = ?",
                change, // used_cash += debit or credit
                change, // available_balance += debit or credit
                lastAmount,
                id
        );
    }

    public BigDecimal getCurrentAvailableBalance(String hierarchyType, String id, String walletType) {
        String tableName = commonMethods.getAmountTable(hierarchyType, walletType);

        try {
            return jdbcTemplate.queryForObject(
                    "SELECT available_balance FROM " + tableName + " WHERE id = ?",
                    new Object[]{id},
                    BigDecimal.class
            );
        } catch (Exception e) {
            throw new RuntimeException("Balance not found for ID: " + id);
        }
    }

    private Optional<String> findParentHierarchy(String hierarchyId) {
        String hierarchyType = commonMethods.extractHierarchyType(hierarchyId);

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
}
