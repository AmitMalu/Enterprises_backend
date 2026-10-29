package com.scs.admin.response;

/**
 * @author SHUBHAM KHOPADE
 */
import lombok.Data;

@Data
public class ShadvalPayCreateOrderResponse {

    private String status;
    private String code;
    private String message;
    private DataNode data;

    @Data
    public static class DataNode {
        private String payment_id;
        private String payment_url;
        private String qr_intent;
        private String qr_path;
    }
}