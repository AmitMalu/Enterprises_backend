package com.im.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Unified response DTO for both AdminUser and WhiteLabelUser.
 * The userType field tells the caller which table the record came from.
 *
 * @author SHUBHAM KHOPADE
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserResponseDTO {

    /** "AD" for AdminUser, "WL" for WhiteLabelUser */
    private String userType;

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
