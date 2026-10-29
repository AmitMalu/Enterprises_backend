package com.scs.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DmtResponseDto {

    private String orderId;         // internal order ID
    private String vendorTxnId;     // vendor transaction ID / ackno
    private BigDecimal amount;      // transaction amount
    private String status;          // SUCCESS / PENDING / FAILED
    private String message;         // human-readable message
    private String beneficiaryName; // optional from vendor response
    private String remarks;         // extra remarks if any
    private String utr;             // bank UTR number
    private String referenceId;     // vendor reference ID
    private BigDecimal balance;     // remaining wallet balance
    private String ipay_uuid;

    // Convenience constructor for direct mapping
    public DmtResponseDto(RemitterTransactionResponse resp, String orderId, BigDecimal amount, String status) {
        this.orderId = orderId;
        this.vendorTxnId = resp.getAckno();
        this.amount = amount;
        this.status = status;
        this.message = resp.getMessage();
        this.beneficiaryName = resp.getBenename();
        this.remarks = resp.getRemarks();
        this.utr = resp.getUtr();
        this.referenceId = resp.getReferenceid() != null ? String.valueOf(resp.getReferenceid()) : null;
        this.balance = resp.getBalance() != null ? BigDecimal.valueOf(resp.getBalance()) : null;
    }
}
