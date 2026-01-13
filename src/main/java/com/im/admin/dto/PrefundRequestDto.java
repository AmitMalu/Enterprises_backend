package com.im.admin.dto;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PrefundRequestDto {

    private String requestedBy;
    private String mobileNumber;
    private String depositAmount;
    private String confirmAmount;
    private String depositImage;
    private String bankHolderName;
    private String bankAccountName;
    private String bankAccountNumber;
    private String bankTranId;
    private String narration;
    private String paymentMode;
    private String ipAddress;
    private String depositDate;
    private String depositType;
    private String remarks;

}
