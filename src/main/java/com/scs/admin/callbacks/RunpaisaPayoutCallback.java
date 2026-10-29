package com.scs.admin.callbacks;

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
//    private String order_id; // new payout
     private String orderid;// old_payout - current

//    private String utr_no; // new payout
      private String utrno; // old_payout - current
    private String txn_amount;

}

