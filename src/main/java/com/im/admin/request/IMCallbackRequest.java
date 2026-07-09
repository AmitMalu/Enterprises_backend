package com.im.admin.request;

/**
 * @author SHUBHAM KHOPADE
 */
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@AllArgsConstructor
@NoArgsConstructor
public class IMCallbackRequest {

    private String txid;

    private String status;

    @JsonProperty("product")
    private String product;

    @JsonProperty("operator")
    private String operator;

    @JsonProperty("tx_ref")
    private String transactionReference;

    @JsonProperty("failure_reason")
    private String failureReason;

}
