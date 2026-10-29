package com.scs.admin.request;

import com.scs.admin.entities.SchemeRate;
import com.scs.admin.entities.enums.WalletType;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class DmtTransactionProcessingDto {

    private String baseTranxId;
    private BigDecimal amount;
    private BigDecimal confirmAmount;
    private String operatorName;
    private String operatorCode;
    private SchemeRate schemeRate;
    private String transactionType;
    private WalletType walletType;
    private String gstValue;
    private String tdsRate;
    private String productCode;
    private String subProductCode;
    private Boolean isCommission;
    private Boolean hasCharge;
    private String hierarchyId;
}
