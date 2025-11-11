//package com.im.admin.dto;
//
//import lombok.Data;
//
//import java.math.BigDecimal;
//
//@Data
//public class DmtResponseDto {
//    private String orderId;
//    private String vendorTxnId;
//    private BigDecimal amount;
//    private String status;
//    private String message;
//
//    public DmtResponseDto(RemitterTransactionResponse resp) {
//        this.vendorTxnId = resp.getTxnId();
//        this.message = resp.getMessage();
//        this.status = resp.isStatus() ? "SUCCESS" : "FAILED";
//    }
//}
