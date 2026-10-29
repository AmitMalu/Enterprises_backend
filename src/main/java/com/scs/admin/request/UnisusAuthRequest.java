package com.scs.admin.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UnisusAuthRequest {

    @JsonProperty("APIKey")
    private String apiKey;

    @JsonProperty("MobileNo")
    private String mobileNo;

    @JsonProperty("AgentID")
    private String agentId;
}
