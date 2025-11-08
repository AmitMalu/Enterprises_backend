package com.im.admin.response;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
public class BeneficiaryRegisterResponse {

    private boolean status;
    private int response_code;
    private String message;
    private Data data;

    @Getter
    @Setter
    public static class Data {
        private Long bene_id;
        private String bankid;
        private String bankname;
        private String name;
        private String accno;
        private String ifsc;
        private String verified;
        private int status;
    }
}
