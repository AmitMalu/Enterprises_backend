package com.scs.admin.response;

/**
 * @author SHUBHAM KHOPADE
 */
import lombok.Data;

@Data
public class NixaSoftPayoutResponse {

    private String statuscode;
    private String message;
    private NixaSoftPayoutData data;
}
