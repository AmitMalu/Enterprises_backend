package com.scs.admin.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class UnisusWebhookRequest {

    @JsonProperty("RequestType")
    private Integer requestType;

    @JsonProperty("RequestTime")
    private String requestTime;

    @JsonProperty("RequestID")
    private Long requestID;

    @JsonProperty("SessionID")
    private Long sessionID;

    @JsonProperty("MobileNo")
    private String mobileNo;

    @JsonProperty("AgentID")
    private String agentID;

    @JsonProperty("OtherInfo")
    private OtherInfo otherInfo;

    @JsonProperty("Transaction")
    private Transaction transaction;
}
