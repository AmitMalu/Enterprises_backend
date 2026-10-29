package com.scs.admin.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PayoutMerchantCallbackResponse {

    private String code;
    private String status;
    private String message;
    private Object data;
    private List<Map<String, String>> errors;

}
