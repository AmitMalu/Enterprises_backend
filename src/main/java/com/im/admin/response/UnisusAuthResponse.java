package com.im.admin.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class UnisusAuthResponse {

    @JsonProperty("Status")
    private int status;

    @JsonProperty("StatusMsg")
    private String statusMsg;

    @JsonProperty("SessionID")
    private String sessionId;

    @JsonProperty("AccessKey")
    private String accessKey;
}
