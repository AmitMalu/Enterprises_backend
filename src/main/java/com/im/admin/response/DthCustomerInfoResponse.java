package com.im.admin.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

/**
 * author abdul.M
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class DthCustomerInfoResponse {

    private Integer statusCode;
    private String msg;
    private DataResponse data;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DataResponse {

        private Integer statusCode;
        private String msg;
        private List<CustomerInfo> data;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CustomerInfo {

        private String key;
        private String value;
        private Integer ind;
    }
}