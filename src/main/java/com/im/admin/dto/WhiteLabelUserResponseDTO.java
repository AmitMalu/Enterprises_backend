package com.im.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * @author SHUBHAM KHOPADE
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class WhiteLabelUserResponseDTO {

    private String id;
    private String parentAccount;

    private String abbreviation;

    private String name;
    private String mobileNumber;
    private String username;
    private String emailId;

    private Boolean emailVerified;
    private LocalDate dob;

    private AddressDTO add;

    private Boolean status;

    private LocalDateTime createdOn;
    private String createdBy;
    private LocalDateTime lastEditedOn;
    private String editedBy;
}
