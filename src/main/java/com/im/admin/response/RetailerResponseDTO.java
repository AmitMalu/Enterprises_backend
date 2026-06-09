package com.im.admin.response;

import com.im.admin.dto.AddressDTO;
import com.im.admin.dto.BankDetailsDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RetailerResponseDTO {
    private String retailerId;
    private String name;
    private String mobileNumber;
    private String emailId;
    private String title;
    private String abbreviation;
    private LocalDate dob;

    private AddressDTO address;
    private AddressDTO outletAddress;
    private BankDetailsDTO bankDetails;

    private String aadhaarNumber;
    private String panNumber;
    private String shopNumber;
    private String businessNumber;
    private String status;
    private String remarks;
    private String rejectionReason;

    private String aadhaarCardFrontImage;
    private String aadhaarCardBackImage;
    private String panCardImage;
    private String shopImage;
    private String businessProofImage;

    private String createdBy;
    private String createdByIPAddress;
    private LocalDateTime createdOn;
    private LocalDateTime lastEditedOn;

    private String latitude;
    private String longitude;


}