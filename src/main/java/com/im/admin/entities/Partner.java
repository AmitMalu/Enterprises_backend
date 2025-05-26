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
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "partner")
public class Partner {

    @Id
    @Column(name = "partner_id", nullable = false, length = 15)
    private String partnerId;

    @Column(name = "parent_account", nullable = false)
    private String parentAccount;

    @NotBlank(message = "Abbreviation is required")
    @Column(name = "abbreviation", nullable = false, unique = true, length = 3)
    @Size(min = 2, max = 3, message = "Abbreviation must be between 2 and 3 characters")
    @Pattern(regexp = "^[A-Z]{2,3}$", message = "Abbreviation must be 2 to 3 uppercase letters (no numbers or special characters)")
    private String abbreviation;

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

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dob;

    @NotBlank(message = "Outlet Name is mandatory")
    private String outletName;

    private String companyType;

    @Column(name = "competitor")
    private String competitor;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "address", column = @Column(name = "main_address")),
            @AttributeOverride(name = "city", column = @Column(name = "main_city")),
            @AttributeOverride(name = "state", column = @Column(name = "main_state")),
            @AttributeOverride(name = "zipCode", column = @Column(name = "main_zip_code"))
    })
    @Valid
    private Address address;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "address", column = @Column(name = "outlet_address")),
            @AttributeOverride(name = "city", column = @Column(name = "outlet_city")),
            @AttributeOverride(name = "state", column = @Column(name = "outlet_state")),
            @AttributeOverride(name = "zipCode", column = @Column(name = "outlet_zip_code"))
    })
    @Valid
    private Address outletAddress;

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

    @Column(name = "pan_card_image")
    private String panCardImage;

    @Column(name = "shop_image")
    private String shopImage;

    @Column(name = "business_proof_image")
    private String businessProofImage;

}