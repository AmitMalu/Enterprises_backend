package com.im.admin.request;

import lombok.Data;

@Data
public class UnisusWebhookRequest {

    private Integer RequestType;
    private String RequestTime;
    private Long RequestID;
    private Long SessionID;
    private String MobileNo;
    private String AgentID;

    private OtherInfo OtherInfo;
    private Transaction Transaction;
}
