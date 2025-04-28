package com.im.admin.dto;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class WalletLoadRequestDto {

    private String hierarchyId;
    private String parentHierarchyId;
    private String mobileNumber;
    private String depositAmount;
    private String depositImage;
    private String bankAccountName;
    private String bankAccountNumber;
    private String bankTranId;
    private String paymentMode;

}
