package com.scs.admin.request;

/**
 * @author SHUBHAM KHOPADE
 */
import lombok.Data;

@Data
public class DigilockerInitRequest {

    private boolean signupFlow;
    private String logoUrl;
    private String redirectUrl;
    private boolean skipMainScreen;

}
