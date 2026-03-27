package com.im.admin.response;

import lombok.Data;

import java.util.List;

@Data
public class OperatorResponse {
    private List<OperatorDto> data;
    private Integer statusCode;
    private String msg;

    @Data
    public  static class OperatorDto {
        private String serviceName;
        private String operatorName;
        private String spKey;
    }
}
