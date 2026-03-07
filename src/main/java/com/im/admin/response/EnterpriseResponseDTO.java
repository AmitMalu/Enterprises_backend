package com.im.admin.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class EnterpriseResponseDTO {

    private String enterpriseId;
    private String parentAccount;
    private String name;
    private String mobileNumber;
    private String emailId;
    private String title;
    private String abbreviation;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dob;

    private String status;
    private String domainURL;
    private String businessDesc;

    private String panNumber;
    private String aadhaarNumber;

    private Boolean panVerified;
    private Boolean aadhaarPanVerified;

    private String createdBy;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdOn;

}
