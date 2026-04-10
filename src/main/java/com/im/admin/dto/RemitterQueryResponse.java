package com.im.admin.dto;

import lombok.Data;

@Data
public class RemitterQueryResponse {
    private boolean status;
    private int response_code;
    private String message;
    private DataObj data;
    private String vendor_flow;

    @Data
    public static class DataObj {
        private String mobile;
        private String ekyc_id;
        private String stateresp;
        private String referenceKey;
        private Double limit;
    }
}

