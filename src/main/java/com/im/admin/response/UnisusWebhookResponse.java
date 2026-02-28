package com.im.admin.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UnisusWebhookResponse {

    private Integer Status;
    private String StatusMsg;
    private String TrnRefNo;
}
