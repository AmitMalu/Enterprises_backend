package com.im.admin.request;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

/**
 * @author SHUBHAM KHOPADE
 */

@Getter
@Setter
public class RazorPayCreateOrderRequest {

    private Integer amount;                 // in paise (1000 = ₹10)
    private String currency;                // INR

    private Boolean acceptPartial;
    private Integer firstMinPartialAmount;

    private Long expireBy;                 // timestamp

    private String referenceId;
    private String description;

    private Customer customer;
    private Notify notify;

    private Boolean reminderEnable;

    private Map<String, String> notes;

    private String callbackUrl;
    private String callbackMethod;
    private String initiatedBy;
    private Short productId;
    private String subProductId;
    private String cardBinNo;

}
