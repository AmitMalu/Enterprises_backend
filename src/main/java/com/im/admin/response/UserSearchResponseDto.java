package com.im.admin.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserSearchResponseDto {
    private String mobileNumber;
    private String hierarchyId;
    private String hierarchyCode;
    private LocalDate createdDate;
    private LocalDate lastLoginDate;
    private String emailId;

}


