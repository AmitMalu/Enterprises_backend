package com.im.admin.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class BillAvenueBillPayResponse {

    private String responseCode;
    private String responseReason;
    private String txnRefId;
    private String txnRespType;
    private String respAmount;
    private String respBillNumber;
    private String respCustomerName;
    private String respDueDate;
    private String approvalRefNumber;
    private String requestId;


    @JsonProperty("vErrorRootVO")
    private VErrorRootVO vErrorRootVO;

    @Data
    public static class VErrorRootVO {
        private List<BillAvenueError> error;
    }

    @Data
    public static class BillAvenueError {
        private String errorCode;
        private String errorMessage;
    }
}
