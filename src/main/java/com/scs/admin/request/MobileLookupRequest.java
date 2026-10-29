package com.scs.admin.request;

import lombok.Data;

@Data
public class MobileLookupRequest {
    private String mobileNumber;
    private String subProductId;
}
