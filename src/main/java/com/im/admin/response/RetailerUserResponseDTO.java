package com.im.admin.response;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class RetailerUserResponseDTO {
    private String retailerUserId;
    private String parentAccount;
    private String abbreviation;
    private String name;
    private String mobileNumber;
    private String username;
    private String emailId;
    private Boolean emailVerified;
    private LocalDate dob;
    private Boolean status;
    private LocalDateTime createdOn;
    private String createdBy;
    private LocalDateTime lastEditedOn;
    private String editedBy;
    private String createdByIPAddress;
    private String role_id;
    private String subRoleId;
}
