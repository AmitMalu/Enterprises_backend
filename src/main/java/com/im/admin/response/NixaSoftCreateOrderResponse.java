package com.im.admin.response;

/**
 * @author SHUBHAM KHOPADE
 */
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class NixaSoftCreateOrderResponse {

    @JsonProperty("statuscode")
    private String statusCode;

    @JsonProperty("order_id")
    private String orderId;

    @JsonProperty("amount")
    private String amount;

    @JsonProperty("payment_link")
    private String paymentUrl;

    @JsonProperty("redirect_url")
    private String redirectUrl;
}
