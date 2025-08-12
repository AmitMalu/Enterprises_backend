package com.im.admin.dto;

import com.im.admin.entities.SchemeRate;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class BbpsTranactionRequestDto {

    private SchemeRate schemeRate;
    private Double txnAmount;
    private String walletType;
    private String hierarchyId;
    private String gst;
    private String tds;
    private String vendorResponse;
    private String transactionId;
    private String operatorName;
    private String subProductName;

}
