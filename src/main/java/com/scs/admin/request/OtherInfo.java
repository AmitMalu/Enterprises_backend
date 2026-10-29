package com.scs.admin.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class OtherInfo {

    @JsonProperty("PGTrnType")
    private String pgTrnType;

    @JsonProperty("PGTrnSubType")
    private String pgTrnSubType;

    @JsonProperty("PGTrnBankOrCode")
    private String pgTrnBankOrCode;

    @JsonProperty("PGName")
    private String pgName;
}
