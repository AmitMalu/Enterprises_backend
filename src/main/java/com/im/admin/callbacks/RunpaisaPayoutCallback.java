package com.im.admin.callbacks;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class RunpaisaPayoutCallback {

    private String code;
    private String status;
    private String message;
    private String order_id;
    private String utr_no;
    private String txn_amount;

}

