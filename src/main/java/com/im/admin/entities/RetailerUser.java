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
@Table(name = "retailer_users")
public class RetailerUser {

    @Id
    @Column(name = "retaileruser_id", nullable = false)
    private String retailerUserId;

    @Column(name = "parent_account", nullable = false)
    private String parentAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "retailer_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Retailer retailer;

    @Column(name = "abbreviation", length = 3)
    @Size(min = 2, max = 3, message = "Abbreviation must be between 2 and 3 characters")
    @Pattern(regexp = "^[A-Z]{2,3}$", message = "Abbreviation must be 2 to 3 uppercase letters (no numbers or special characters)")
    private String abbreviation;

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

    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bank_details_id", referencedColumnName = "id")
    private BanksDetailss bankDetails;

    @Column(name = "status")
    private Boolean status = true;

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

    }

}
