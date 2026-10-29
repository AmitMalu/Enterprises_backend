package com.scs.admin.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.scs.admin.dto.BankDetailsDTO;
import com.scs.admin.entities.Address;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class EnterpriseResponseDTO {

//    private String enterpriseId;
//    private String parentAccount;
//    private String name;
//    private String mobileNumber;
//    private String emailId;
//    private String title;
//    private String abbreviation;
//
//    @JsonFormat(pattern = "yyyy-MM-dd")
//    private LocalDate dob;
//
//    private String status;
//    private String domainURL;
//    private String businessDesc;
//
//    private String panNumber;
//    private String aadhaarNumber;
//
//    private Boolean panVerified;
//    private Boolean aadhaarPanVerified;
//
//    private String createdBy;
//
//    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
//    private LocalDateTime createdOn;

    private String status;
    private String enterpriseId;
    private String parentAccount;
    private String name;
    private String mobileNumber;
    private String emailId;
    private String title;
    private String abbreviation;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dob;

    private String domainURL;
    private String businessDesc;

    private String panNumber;
    private String aadhaarNumber;

    private String shopNumber;
    private String businessNumber;

    private Boolean panVerified;
    private Boolean aadhaarPanVerified;
    private Boolean isAadhaarVerified;
    private Boolean emailVerified;
    private Boolean gstNumberVerified;

    private String createdBy;
    private String createdByIPAddress;

    private Address address;
    private Address outletAddress;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private LocalDateTime createdOn;

    private String aadhaarCardFrontImage;
    private String aadhaarCardBackImage;
    private String panCardImage;
    private String shopImage;
    private String businessProofImage;

    private BankDetailsDTO bankDetails;


}
