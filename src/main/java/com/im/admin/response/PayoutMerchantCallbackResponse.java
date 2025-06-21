package com.im.admin.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PayoutMerchantCallbackResponse {

    private String code;
    private String status;
    private String message;
    private List<String> errors;
    private Object data;
}
