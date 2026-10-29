package com.scs.admin.request;

import lombok.Data;

@Data
public class BillFetchRequest {
    private String agentId;
    private String requestId;
    private boolean billerAdhoc;
    private String subProductId;

    private AgentDeviceInfo agentDeviceInfo;
    private CustomerInfo customerInfo;

    private String billerId;
    private InputParams inputParams;

    @Data
    public static class AgentDeviceInfo {
        private String ip;
        private String initChannel;
        private String mac;
    }

    @Data
    public static class CustomerInfo {
        private Long customerMobile;
        private String customerEmail;
        private String customerAdhaar;
        private String customerPan;
    }

    @Data
    public static class InputParams {
        private java.util.List<Input> input;

        @Data
        public static class Input {
            private String paramName;
            private String paramValue;
        }
    }
}
