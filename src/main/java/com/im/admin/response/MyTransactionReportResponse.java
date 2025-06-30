package com.im.admin.response;

import com.im.admin.entities.enums.Status;
import com.im.admin.entities.enums.WalletType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MyTransactionReportResponse {

    private Integer id;   //0
    private LocalDate transactionDate;  //1
    private LocalTime transactionTime;  //2
    private BigDecimal balBefore;  //3
    private BigDecimal amount;   //4
    private BigDecimal balAfter;  //5
    private String subProductName;  //6
    private String actionOnBalanceAmount;   //7
    private Status status;   //8
    private String tranxId;  //9
    private String narration;  //10
    private WalletType walletType;


}
