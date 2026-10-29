package com.scs.admin.request;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import com.fasterxml.jackson.databind.JsonNode;

@Data
public class BillPaymentRequest {

    private String requestId;
    private String retailerId;
    private String subProductId;
    private String agentId;
    private boolean billerAdhoc;
    private String number;

    private AgentDeviceInfo agentDeviceInfo;
    private CustomerInfo customerInfo;

    private String billerId;

    private InputParams inputParams;
//    private BillerResponse billerResponse;

    private JsonNode billerResponse;


    private String paymentRefId;
    private String schemeCode;
    private String txnType;

    private AmountInfo amountInfo;
    private PaymentMethod paymentMethod;
    private PaymentInfo paymentInfo;

    //  MISSING FIELD ADDED
    private AdditionalInfo additionalInfo;

    private String medium;
    private String requestDomainName;

    // ===================== INNER CLASSES ======================

    @Data
    public static class AgentDeviceInfo {
        private String ip;
        private String initChannel;
        private String mac;
    }

    @Data
    public static class CustomerInfo {
        private String customerMobile;
        private String customerEmail;
        private String customerAdhaar;
        private String customerPan;
    }

    @Data
    public static class InputParams {
        private List<Input> input;

        @Data
        public static class Input {
            private String paramName;
            private String paramValue;
        }
    }

//    @Data
//    public static class BillerResponse {
//        private String billAmount;     // ⚠ STRING as per JSON
//        private String billDate;
//        private String billNumber;
//        private String billPeriod;
//        private String customerName;
//        private String dueDate;
//    }

    @Data
    public static class AmountInfo {
        private BigDecimal amount;
        private int currency;
        private long custConvFee;
        private List<String> amountTags;
    }

    @Data
    public static class PaymentMethod {
        private String paymentMode;
        private String quickPay;
        private String splitPay;
    }

    @Data
    public static class AdditionalInfo {
        private List<Info> info;

        @Data
        public static class Info {
            private String infoName;
            private String infoValue;
        }
    }

    @Data
    public static class PaymentInfo {
        private List<Info> info;

        @Data
        public static class Info {
            private String infoName;
            private String infoValue;
        }
    }
}
