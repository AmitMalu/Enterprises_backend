package com.scs.admin.request;

import lombok.Data;

@Data
public class RechargePlanRequest {
    private Integer spkey;
    private Integer circleId;
    private String subProductId;

}
