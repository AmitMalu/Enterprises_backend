package com.im.admin.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@AllArgsConstructor
public class ChildUserDto {

    private String network;         //RETAILER, PARTNER, SUPER DISTRIBUTOR etc
    private String userId;         // example: partnerId / retailerId
    private String parentAccount;
    private String abbreviation;
    private String title;
    private String name;
    private String mobileNumber;
    private String emailId;
    private LocalDate dob;
    private String status;
    private Boolean isBlocked;
    private String remarks;
    private LocalDateTime createdOn;

    private Map<String, Object> walletInformation;

}



