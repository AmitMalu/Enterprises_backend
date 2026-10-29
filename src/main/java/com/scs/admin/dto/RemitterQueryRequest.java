package com.scs.admin.dto;

import lombok.Data;

@Data
public class RemitterQueryRequest {
    private String mobile;
    private String subProductId;
    private String retailerId;
}
