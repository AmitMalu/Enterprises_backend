package com.im.admin.request;

import lombok.Data;

@Data
public class RechargePlanRequest {
    private Integer spkey;
    private Integer circleId;
    private String subProductId;

}
