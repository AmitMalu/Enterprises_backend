package com.im.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.time.LocalDate;

@Data
@AllArgsConstructor
public class ChildUserDto {
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
}
