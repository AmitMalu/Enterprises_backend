package com.im.admin.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class DistributorResponseDTO {

    private String distributorId;
    private String parentAccount;
    private String title;
    private String name;
    private String mobileNumber;
    private String emailId;
    private String abbreviation;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dob;

    private String status;
    private String remarks;

    private String aadhaarNumber;
    private String panNumber;

    private Boolean panVerified;
    private Boolean aadhaarPanVerified;

    private String shopNumber;
    private String businessNumber;

    private String createdBy;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private LocalDateTime createdOn;

}
