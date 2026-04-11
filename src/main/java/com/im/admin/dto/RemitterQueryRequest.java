package com.im.admin.dto;

import lombok.Data;

@Data
public class RemitterQueryRequest {
    private String mobile;
    private String subProductId;
    private String retailerId;
}
