package com.scs.admin.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.io.StringReader;
import java.time.LocalDate;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StatusWiseUserFullDto {

    private String adminId;
    private String enterpriseId;
    private String whiteLabelId;
    private String superDistributorId;
    private String partnerId;
    private String distributorId;
    private String retailerId;

    private String parentAccount;
    private String abbreviation;
    private String title;
    private String name;
    private String mobileNumber;
    private String emailId;
    private LocalDate dob;
    private String status;
    private Boolean isBlocked;
}
