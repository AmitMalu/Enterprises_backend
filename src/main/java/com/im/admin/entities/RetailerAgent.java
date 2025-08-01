package com.im.admin.entities;


import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "retailer_agents")
public class RetailerAgent {

     @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) //
    @Column(name = "agent_id")
    private Long agentId;

    @Column(name = "parent_account", nullable = false)
    private String parentAccount;

//    @Column(name = "abbreviation", length = 3)
//    @Size(min = 2, max = 3, message = "Abbreviation must be between 2 and 3 characters")
//    @Pattern(regexp = "^[A-Z]{2,3}$", message = "Abbreviation must be 2 to 3 uppercase letters (no numbers or special characters)")
//    private String abbreviation;

//    @NotBlank(message = "Title is mandatory")
//    private String title;

    @NotBlank(message = "Name is mandatory")
    private String name;

    @NotBlank(message = "Mobile Number is mandatory")
    @Column(name = "mobile_number", nullable = false, unique = true)
    @Pattern(regexp = "^[0-9]{10}$", message = "Mobile number must be 10 digits")
    private String mobileNumber;

    @Column(name = "username")
    private String username;

    @Column(name = "password")
    private String password;

    @NotBlank(message = "Email is mandatory")
    @Email(message = "Invalid email format")
    private String emailId;

    @Column(name = "email_verified")
    private Boolean emailVerified;

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
    private Address add;

//    @Embedded
//    @AttributeOverrides({
//            @AttributeOverride(name = "outletName", column = @Column(name = "outlet_name")),
//            @AttributeOverride(name = "companyType", column = @Column(name = "outlet_company_type")),
//            @AttributeOverride(name = "companyName", column = @Column(name = "outlet_company_name")),
//            @AttributeOverride(name = "address", column = @Column(name = "outlet_address")),
//            @AttributeOverride(name = "city", column = @Column(name = "outlet_city")),
//            @AttributeOverride(name = "state", column = @Column(name = "outlet_state")),
//            @AttributeOverride(name = "zipCode", column = @Column(name = "outlet_zip_code"))
//    })
//    private Address outletAddress;

    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "bank_details_id", referencedColumnName = "id")
    private BanksDetailss bankDetails;

    @Column(name = "status")
    private Boolean status = true;

//    @Column(name = "aadhaar_card_front_image")
//    private String aadhaarCardFrontImage;
//
//    @Column(name = "aadhaar_card_back_image")
//    private String aadhaarCardBackImage;
//
//    @Column(name = "is_aadhaar_verified")
//    private Boolean isAadhaarVerified;
//
//    @Column(name = "aadhaar_number")
//    private String aadhaarNumber;
//
//    @Column(name = "pan_card_image")
//    private String panCardImage;
//
//    @Column(name = "pan_number")
//    private String panNumber;
//
//    @Column(name = "pan_verified")
//    private Boolean panVerified;
//
//    @Column(name = "aadhaar_pan_verified")
//    private Boolean aadhaarPanVerified;
//
//    @Column(name = "shop_image")
//    private String shopImage;
//
//    @Column(name = "shop_number")
//    private String shopNumber;
//
//    @Column(name = "business_proof_image")
//    private String businessProofImage;
//
//    @Column(name = "business_number")
//    private String businessNumber;
//
//    @Column(name = "GST_number_verified")
//    private Boolean GSTNumberVerified;

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

    @Column(name = " role_id")
    private String  role_id;

    @Column(name = "sub_role_id")
    private  String subRoleId;

    @PrePersist
    public void prePersistDefaults() {
        if (this.add != null) {
            if (this.add.getOutletName() == null) {
                this.add.setOutletName("N/A");
            }
            if (this.add.getCompanyType() == null) {
                this.add.setCompanyType("N/A");
            }
            if (this.add.getCompanyName() == null) {
                this.add.setCompanyName("N/A");
            }
            if (this.add.getAddress() == null) {
                this.add.setAddress("N/A");
            }
            if (this.add.getCity() == null) {
                this.add.setCity("N/A");
            }
            if (this.add.getState() == null) {
                this.add.setState("N/A");
            }
            if (this.add.getZipCode() == null) {
                this.add.setZipCode("N/A");
            }
        }



//        if (this.outletAddress != null) {
//            if (this.outletAddress.getOutletName() == null) {
//                this.outletAddress.setOutletName("N/A");
//            }
//            if (this.outletAddress.getCompanyType() == null) {
//                this.outletAddress.setCompanyType("N/A");
//            }
//            if (this.outletAddress.getCompanyName() == null) {
//                this.outletAddress.setCompanyName("N/A");
//            }
//            if (this.outletAddress.getAddress() == null) {
//                this.outletAddress.setAddress("N/A");
//            }
//            if (this.outletAddress.getCity() == null) {
//                this.outletAddress.setCity("N/A");
//            }
//            if (this.outletAddress.getState() == null) {
//                this.outletAddress.setState("N/A");
//            }
//            if (this.outletAddress.getZipCode() == null) {
//                this.outletAddress.setZipCode("N/A");
//            }
//        }
    }

}
