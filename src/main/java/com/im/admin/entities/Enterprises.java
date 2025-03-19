package com.im.admin.entities;

import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.validator.constraints.URL;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "enterprises")
public class Enterprises {

    @Id
    @Column(name = "enterprise_id", unique = true, nullable = false, length = 15)
    private String enterpriseId;  // Format: ABC-ET-001

    @NotBlank(message = "Parent account is required")
    @Column(name = "parent_account")
    private String parentAccount;

    @NotBlank(message = "Company name is required")
    @Column(name = "company_name")
    private String companyName;

    @NotBlank(message = "Mobile is mandatory")
    @Pattern(regexp = "^[0-9]{10}$", message = "Mobile number must be 10 digits")
    @Column(name = "mobile_no")
    private String mobileNo;

    @NotBlank(message = "Abbreviation is required")
    @Column(name = "abbreviation", nullable = false, unique = true, length = 3)
    @Size(min = 2, max = 3, message = "Abbreviation must be between 2 and 3 characters")
    @Pattern(regexp = "^[A-Z]{2,3}$", message = "Abbreviation must be 2 to 3 uppercase letters (no numbers or special characters)")
    private String abbreviation;

    @Column(name = "company_type")
    private String companyType;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "address", column = @Column(name = "main_address")),
            @AttributeOverride(name = "city", column = @Column(name = "main_city")),
            @AttributeOverride(name = "state", column = @Column(name = "main_state")),
            @AttributeOverride(name = "zipCode", column = @Column(name = "main_zip_code"))
    })
    @Valid
    private Address registeredAddress;

    @Column(name = "business_desc")
    private String businessDesc;

    @URL(message = "Invalid website URL")
    @Column(name = "website_url")
    private String websiteURL;

    @Pattern(regexp = "[A-Z]{5}[0-9]{4}[A-Z]{1}", message = "Invalid PAN card number")
    @Column(name = "pan_card_number")
    private String panCardNumber;

    @Pattern(regexp = "\\d{2}[A-Z]{5}\\d{4}[A-Z]{1}\\d{1}[A-Z]{1}\\d{1}", message = "Invalid GST number")
    @Column(name = "gst_number")
    private String GSTNumber;

    @NotBlank(message = "Bank account details are required")
    @Column(name = "bank_account_details")
    private String bankAccountDetails;

    @Column(name = "financial_statements")
    private String financialStatements;

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
}
