package com.scs.admin.response;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class KYCResponseDto {

    private String hierarchyId;
    private String name;
    private String mobileNumber;
    private String emailId;
//    private Boolean emailVerified;
//
//    private String aadhaarCardFrontImage;
//    private String aadhaarCardBackImage;
//    private Boolean isAadhaarVerified;
//    private String aadhaarNumber;
//
//    private String panCardImage;
//    private String panNumber;
//    private Boolean panVerified;
//    private Boolean aadhaarPanVerified;
//
//    private String shopImage;
//    private String shopNumber;
//    private String businessProofImage;
//    private String businessNumber;
//    private Boolean gstNumberVerified;
}
