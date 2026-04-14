package com.im.admin.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class WhiteLabelResponseDTO {

    private String whiteLabelId;
    private String title;
    private String name;
    private String mobileNumber;
    private String emailId;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dob;

    private String businessDesc;
    private String abbreviation;
    private String parentAccount;

    private String domainURL;
    private Boolean isAdmin;

    private String status;
    private String rejectionReason;

    private String aadhaarNumber;
    private String panNumber;

    private Boolean panVerified;
    private Boolean aadhaarPanVerified;

    private Boolean emailVerified;
    private Boolean isAadhaarVerified;
    private Boolean gstNumberVerified;

    private String shopNumber;
    private String businessNumber;

    private String remarks;

    private String createdBy;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private LocalDateTime createdOn;

    private String aadhaarCardFrontImage;
    private String aadhaarCardBackImage;
    private String panCardImage;
    private String shopImage;
    private String businessProofImage;

}
