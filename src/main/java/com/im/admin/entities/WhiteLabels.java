package com.im.admin.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;


@Entity
@Getter
@Setter
@Table(name = "white_labels")
public class WhiteLabels {

    @Id
    @Column(name = "whitelabel_id", nullable = false, length = 15)
    private String whiteLabelId;  // Format: ABC-WL-001

    @Column(name = "IM_account_member_ID")
    private String imAccountMemberID;

    @Column(name = "title", nullable = false)
    private String title;

    @NotBlank(message = "Mobile Number is mandatory")
    @Column(name = "mobile_number", nullable = false, unique = true)
    @Pattern(regexp = "^[0-9]{10}$", message = "Mobile number must be 10 digits")
    private String mobileNumber;

    @NotBlank(message = "Email is mandatory")
    @Email(message = "Invalid email format")
    private String emailId;

    @NotBlank(message = "Abbreviation is required")
    @Column(name = "abbreviation", nullable = false, unique = true, length = 3)
    @Size(min = 2, max = 3, message = "Abbreviation must be between 2 and 3 characters")
    @Pattern(regexp = "^[A-Z]{2,3}$", message = "Abbreviation must be 2 to 3 uppercase letters (no numbers or special characters)")
    private String abbreviation;

    @Column(name = "address")
    private String address;

    @NotBlank(message = "Parent account is required")
    @Column(name = "parent_account")
    private String parentAccount;

    @Column(name = "domain_url")
    private String domainURL;

    @Column(name = "terms_and_condition_url")
    private String termsAndConditionURL;

    @Column(name = "privacy_policy_url")
    private String privacyPolicyURL;

    @Column(name = "refund_and_cancellation_url")
    private String refundAndCancellationURL;

    @Column(name = "is_service_allow", nullable = false)
    private Boolean isServiceAllow = false;

    @Column(name = "allow_prepaid_cards")
    private Boolean allowPrepaidCards;

    @Column(name = "prepaid_card_name")
    private String prepaidCardName;

    @Column(name = "allow_postpaid_cards")
    private Boolean allowPostpaidCards;

    @Column(name = "postpaid_card_name")
    private String postpaidCardName;

    @Column(name = "products", columnDefinition = "TEXT")
    private String products; // Store as a comma-separated string

    @Column(name = "is_redirect")
    private Boolean isRedirect;

    @Column(name = "appID")
    private String appID;

    @Column(name = "captcha_site_key")
    private String captchaSiteKey;

    @Column(name = "captcha_secret_key")
    private String captchaSecretKey;

    @Column(name = "remarks")
    private String remarks;

    @ManyToOne
    @JoinColumn(name = "theme_id", nullable = true)
    private Theme theme;

    @CreationTimestamp
    @Column(name = "created_on", nullable = false, updatable = false)
    private LocalDateTime createdOn;

    @Column(name = "created_by", nullable = false)
    private String createdBy;

    @UpdateTimestamp
    @Column(name = "edited_on")
    private LocalDateTime editedOn;

    @Column(name = "edited_by")
    private String editedBy;

    @Column(name = "is_admin")
    private Boolean isAdmin;

    @Column(name = "status")
    private String status = "PENDING";

    @Column(name = "rejection_reason")
    private String rejectionReason = "-";

    @Column(name = "aadhaar_card_front_image")
    private String aadhaarCardFrontImage;

    @Column(name = "aadhaar_card_back_image")
    private String aadhaarCardBackImage;

    @Column(name = "pan_card_image")
    private String panCardImage;

    @Column(name = "shop_image")
    private String shopImage;

    @Column(name = "business_proof_image")
    private String businessProofImage;

}
