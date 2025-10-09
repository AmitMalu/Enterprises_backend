package com.im.admin.callbacks;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class RunpaisaPayoutCallback {
    private String status;
    private String message;
    private String remarks;
    private String utr;
    private String rrn;
    private String ref_id;
    private String tx_ref;
    private TxnMethod txn_method;
    private String partner;
    private String created_at;

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TxnMethod {
        private String mode;
        private String card_type;
        private String card_number;
        private String account_id;
        private String partner_json; // This is a stringified JSON
    }
}

