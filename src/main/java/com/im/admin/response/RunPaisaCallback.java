package com.im.admin.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RunPaisaCallback {

    private Long cf_payment_id;
    private String entity;
    private Boolean is_captured;
    private String order_id;
    private Integer order_amount;
    private Integer payment_amount;
    private String payment_completion_time;
    private String payment_currency;
    private String payment_group;
    private String payment_message;
    private String payment_status;
    private String payment_time;

    // Optional fields
    private String auth_id;
    private String authorization;
    private String bank_reference;
    private String error_details;
    private Map<String, Object> payment_gateway_details;
    private Map<String, Object> payment_method;
    private List<Object> payment_offers;

}

