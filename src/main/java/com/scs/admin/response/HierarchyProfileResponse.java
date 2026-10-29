package com.scs.admin.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Unified profile response for any hierarchy member.
 * Returned by GET /api/hierarchies/data?search=<id or mobile>
 *
 * @author SHUBHAM KHOPADE
 */
@Data
@Builder
public class HierarchyProfileResponse {

    // ── Identity ────────────────────────────────────────────────────────────
    private String hierarchyId;
    private String hierarchyType;       // RT, DT, PT, SD, WL, ET, AD, AU, WAU
    private String hierarchyLabel;      // Retailer, Distributor, Partner …
    private String parentAccount;

    // ── Basic Info ──────────────────────────────────────────────────────────
    private String name;
    private String mobileNumber;
    private String emailId;
    private String title;
    private String status;
    private LocalDate dob;
    private LocalDateTime createdOn;
    private String createdBy;
    private LocalDateTime lastEditedOn;
    private String editedBy;

    // ── Login Info (from user_details) ──────────────────────────────────────
    private String loginUserName;
    private String userStatus;          // Active / Pending / Blocked
    private Boolean userLoginStatus;
    private LocalDate lastLoginDate;
    private Boolean isBlocked;
    private String permission;

    // ── Wallet ──────────────────────────────────────────────────────────────
    private WalletInfo prepaidWallet;
    private WalletInfo postpaidWallet;

    // ── Scheme Allocations ──────────────────────────────────────────────────
    private List<SchemeInfo> schemeAllocations;

    // ─────────────────────────────────────────────────────────────────────────
    // Nested DTOs
    // ─────────────────────────────────────────────────────────────────────────

    @Data
    @Builder
    public static class WalletInfo {
        private String walletType;          // PREPAID / POSTPAID
        private BigDecimal totalCash;
        private BigDecimal availableBalance;
        private BigDecimal usedCash;
        private BigDecimal cutOfAmount;
        private BigDecimal lastUpdatedAmount;
        private LocalDate lastUpdatedDate;
    }

    @Data
    @Builder
    public static class SchemeInfo {
        private String productCode;
        private String productName;
        private String subProductCode;
        private String subProductName;
        private String schemeCode;
        private String schemeName;
        private Boolean schemeActive;
    }
}
