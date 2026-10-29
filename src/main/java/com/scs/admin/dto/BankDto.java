package com.scs.admin.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * @author SHUBHAM KHOPADE
 */

@Data
public class BankDto {

    @JsonProperty("BANKID")
    private String bankId;

    @JsonProperty("BANKCODE")
    private String bankCode;

    @JsonProperty("BANKNAME")
    private String bankName;

    @JsonProperty("MIFSCCODE")
    private String mifscCode;

    @JsonProperty("IMPS")
    private String imps;

    @JsonProperty("NEFT")
    private String neft;

    @JsonProperty("STATUS")
    private String status;

}
