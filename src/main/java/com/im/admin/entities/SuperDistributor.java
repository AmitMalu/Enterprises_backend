package com.im.admin.entities;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "super_distributor")
public class SuperDistributor {

    @Id
    @Column(name = "superDistributor_id", unique = true, nullable = false, length = 15)
    private String superDistributorId;  // Format: ABC-SD-001

    @Column(name = "parent_account", nullable = false)
    private String parentAccount;

    @NotBlank(message = "Title is mandatory")
    private String title;

    @NotBlank(message = "Name is mandatory")
    private String name;

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

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dob;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "outletName", column = @Column(name = "main_outlet_name")),
            @AttributeOverride(name = "companyType", column = @Column(name = "main_company_type")),
            @AttributeOverride(name = "companyName", column = @Column(name = "main_company_name")),
            @AttributeOverride(name = "address", column = @Column(name = "main_address")),
            @AttributeOverride(name = "city", column = @Column(name = "main_city")),
            @AttributeOverride(name = "state", column = @Column(name = "main_state")),
            @AttributeOverride(name = "zipCode", column = @Column(name = "main_zip_code"))
    })
    private Address address;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "outletName", column = @Column(name = "outlet_name")),
            @AttributeOverride(name = "companyType", column = @Column(name = "outlet_company_type")),
            @AttributeOverride(name = "companyName", column = @Column(name = "outlet_company_name")),
            @AttributeOverride(name = "address", column = @Column(name = "outlet_address")),
            @AttributeOverride(name = "city", column = @Column(name = "outlet_city")),
            @AttributeOverride(name = "state", column = @Column(name = "outlet_state")),
            @AttributeOverride(name = "zipCode", column = @Column(name = "outlet_zip_code"))
    })
    private Address outletAddress;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "bankName", column = @Column(name = "bank_name")),
            @AttributeOverride(name = "bankAccountNumber", column = @Column(name = "bank_account_number")),
            @AttributeOverride(name = "ifscCode", column = @Column(name = "IFSC_code")),
            @AttributeOverride(name = "branchName", column = @Column(name = "branch_name"))
    })
    private BankDetails bankDetails;

    @Column(name = "status")
    private String status = "PENDING";

    @Column(name = "rejection_reason")
    private String rejectionReason = "-";

    @Column(name = "remarks")
    private String remarks;

    @Column(name = "aadhaar_card_front_image")
    private String aadhaarCardFrontImage;

    @Column(name = "aadhaar_card_back_image")
    private String aadhaarCardBackImage;

    @Column(name = "aadhaar_number")
    private String aadhaarNumber;

    @Column(name = "pan_card_image")
    private String panCardImage;

    @Column(name = "pan_number")
    private String panNumber;

    @Column(name = "shop_image")
    private String shopImage;

    @Column(name = "shop_number")
    private String shopNumber;

    @Column(name = "business_proof_image")
    private String businessProofImage;

    @Column(name = "business_number")
    private String businessNumber;

    @CreationTimestamp
    @Column(name = "created_on", nullable = false, updatable = false)
    private LocalDateTime createdOn;

    @Column(name = "created_by", nullable = false, updatable = false)
    private String createdBy;

    @UpdateTimestamp
    @Column(name = "last_edited_on")
    private LocalDateTime lastEditedOn;

    @Column(name = "last_edited_by")
    private String editedBy;

    @Column(name = "created_by_ip_address", nullable = false, updatable = false)
    private String createdByIPAddress;

    @PrePersist
    public void prePersistDefaults() {
        if (this.address != null) {
            if (this.address.getOutletName() == null) {
                this.address.setOutletName("N/A");
            }
            if (this.address.getCompanyType() == null) {
                this.address.setCompanyType("N/A");
            }
            if (this.address.getCompanyName() == null) {
                this.address.setCompanyName("N/A");
            }
            if (this.address.getAddress() == null) {
                this.address.setAddress("N/A");
            }
            if (this.address.getCity() == null) {
                this.address.setCity("N/A");
            }
            if (this.address.getState() == null) {
                this.address.setState("N/A");
            }
            if (this.address.getZipCode() == null) {
                this.address.setZipCode("N/A");
            }
        }

        if (this.outletAddress != null) {
            if (this.outletAddress.getOutletName() == null) {
                this.outletAddress.setOutletName("N/A");
            }
            if (this.outletAddress.getCompanyType() == null) {
                this.outletAddress.setCompanyType("N/A");
            }
            if (this.outletAddress.getCompanyName() == null) {
                this.outletAddress.setCompanyName("N/A");
            }
            if (this.outletAddress.getAddress() == null) {
                this.outletAddress.setAddress("N/A");
            }
            if (this.outletAddress.getCity() == null) {
                this.outletAddress.setCity("N/A");
            }
            if (this.outletAddress.getState() == null) {
                this.outletAddress.setState("N/A");
            }
            if (this.outletAddress.getZipCode() == null) {
                this.outletAddress.setZipCode("N/A");
            }
        }
    }
}