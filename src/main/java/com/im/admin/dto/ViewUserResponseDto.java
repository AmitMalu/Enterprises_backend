package com.im.admin.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class ViewUserResponseDto {
    private String mobileNumber;
    private String hierarchyId;
    private String hierarchyCode;
    private String loginUserName;
    private Boolean userLoginStatus;
    private LocalDate lastLoginDate;
    private LocalTime lastLoginTime;
    private String userStatus;
    private String emailId;
    private LocalDate createdDate;
    private LocalTime createdTime;
    private String createdBy;
    private LocalDate editedDate;
    private LocalTime editedTime;
    private String editedBy;
    private String roleId;
    private Boolean isBlocked;

    // Extra fields from related table
    private String zipCode;
    private String state;
    private String city;
}
