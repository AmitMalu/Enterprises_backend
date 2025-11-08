package com.im.admin.dto;


import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class DmtBeneficiaryFetchResponse {
    private boolean status;
    private int response_code;
    private String message;
    private List<BeneficiaryData> data;

    @Getter
    @Setter
    public static class BeneficiaryData {
        private String bene_id;
        private String bankid;
        private String bankname;
        private String name;
        private String accno;
        private String ifsc;
        private String verified;
        private String banktype;
        private boolean paytm;
    }
}
