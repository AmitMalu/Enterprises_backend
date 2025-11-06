package com.im.admin.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ShadvalPayResponse {

    private String status;
    private String code;
    private String message;
    private Data data;


    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public class Data {
        private String reference_id;
        private String internal_ref_num;
        private FundAccount fund_account;
        private TransDetails trans_details;
    }


    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public class FundAccount {
        private String name;
        private String ifsc;
        private String account_number;
    }


    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public class TransDetails {
        private String amount;
        private String txn_reference_id;
        private String charge;
    }
}
