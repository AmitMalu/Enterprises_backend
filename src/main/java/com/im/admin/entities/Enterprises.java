package com.im.admin.entities;

import jakarta.persistence.*;
import jakarta.validation.Valid;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "enterprises")
public class Enterprises {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "parent_account", nullable = false)
    private String parentAccount;

    @Column(name = "company_name")
    private String companyName;

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

    @Column(name = "business_description")
    private String businessDesc;

    @Column(name = "website_URL")
    private String websiteURL;

    @Column(name = "pan_card_number")
    private String panCardNumber;

    @Column(name = "GST_number")
    private String GSTNumber;

    @Column(name = "bank_account_details")
    private String bankAccountDetails;

    @Column(name = "financial_statements")
    private String financialStatements;

    @Column(name = "director_name")
    private String directorName;

    @Column(name = "director_email_Id")
    private String directorEmailId;

    @Column(name = "director_mobile_number")
    private String directorMobileNumber;

    @Column(name = "authorized_signatory_name")
    private String authorizedSignatoryName;

    @Column(name = "authorized_signatory_email")
    private String authorizedSignatoryEmail;

    //Documents Columns TODO
    //PANCARD,AADHAR CARD, BANK DEATILS PROOF, GST CERTIFICATE



}