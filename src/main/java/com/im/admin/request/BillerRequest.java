package com.im.admin.request;

import lombok.Data;

import java.util.List;

@Data
public class BillerRequest {
    private List<String> billerId;
    private String subProductId;
}
