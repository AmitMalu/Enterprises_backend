package com.im.admin.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author SHUBHAM KHOPADE
 */

@Data
@AllArgsConstructor
@NoArgsConstructor
public class VidualPayAuthorizationResponse {

    private Boolean success;

    private String message;

    private String responseCode;

    private String decryptedData;
}