package com.im.admin.response;

import lombok.Data;

import java.util.List;

@Data
public class FingpayBankApiResponse  {

        private boolean status;
        private String message;
        private List<BankData> data;

        @Data
        public static class BankData {
            private Integer id;
            private Integer activeFlag;
            private String bankName;
            private String details;
            private String remarks;
            private String timestamp;
            private String iinno;
        }
    }
