package com.im.admin.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AepsCashWithdrawalResponse {
        private boolean status;
        private String message;
        private Long statusCode;
        private CWData data;

        @Data
        public static class CWData {
            private String responseCode;
            private String bankRRN;
            private String fingpayTransactionId;
            private String merchantTxnId;
            private String transactionStatus;
            private Double transactionAmount;
            private Double balanceAmount;
            private String terminalId;
            private String TransactionId;
        }
}
