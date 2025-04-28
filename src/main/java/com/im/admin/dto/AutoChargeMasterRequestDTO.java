package com.im.admin.dto;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class AutoChargeMasterRequestDTO {

    private String retailerId;
    private Long chargeMasterId;
    private String walletType;    // PREPAID or POSTPAID
}
