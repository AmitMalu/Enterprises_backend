package com.im.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * @author SHUBHAM KHOPADE
 */

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class VimoPayCallbackDto {

    private String txnStatus; // "Success", "Failed", "Pending"
    private String txnStatusCode; // "000" success, "001" failed, "002" in progress
    private String txnId; // vendor transaction ID
    private String rrn; // bank RRN number (conditional - only on success)
    private BigDecimal amount;
    private BigDecimal charges;
    private String paymentMode;
    private String paymentPurpose;
    private String merchantRefId;
    private String beneficiaryBank;
    private String beneficiaryAccountNumber;
    private String beneficiaryIFSC;
    private String beneficiaryMobileNumber;
    private String beneficiaryLocation;
    private String beneficiaryName;
    private String responseMessage;
    private String lat;
    private String longitude;
    private String udf1;
    private String udf2;
    private String udf3;
}
