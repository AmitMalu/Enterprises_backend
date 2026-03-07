package com.im.admin.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class CellmobeyRechargeCallback {

    @JsonProperty("ID")
    private String apiCode;

    @JsonProperty("TNO")
    private String transactionRef;

    @JsonProperty("ST")
    private String status;

    @JsonProperty("STMSG")
    private String statusMessage;

    @JsonProperty("TID")
    private String vendorTxnId;

    @JsonProperty("OPRTID")
    private String operatorTxnId;

    @JsonProperty("PRB")
    private String preBalance;

    @JsonProperty("POB")
    private String postBalance;

    @JsonProperty("DP")
    private String discountPercent;

    @JsonProperty("DR")
    private String discountRs;



}
