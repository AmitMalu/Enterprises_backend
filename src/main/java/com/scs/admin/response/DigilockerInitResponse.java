package com.scs.admin.response;

/**
 * @author SHUBHAM KHOPADE
 */
import lombok.Data;

@Data
public class DigilockerInitResponse {

    private DataBody data;
    private int statusCode;
    private String messageCode;
    private String message;
    private boolean success;

    @Data
    public static class DataBody {

        private String client_id;
        private String token;
        private String url;
        private Double expiry_seconds;

    }
}
