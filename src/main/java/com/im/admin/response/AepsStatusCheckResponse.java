package com.im.admin.response;

import lombok.Data;

@Data
public class AepsStatusCheckResponse {
    private boolean onboarded;
    private String stepPending;   // null if completed
}
