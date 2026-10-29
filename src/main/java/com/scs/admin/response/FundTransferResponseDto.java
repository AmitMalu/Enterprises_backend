package com.scs.admin.response;

import com.scs.admin.entities.enums.WalletType;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FundTransferResponseDto {

    private Long id;
    private String parentHierarchyId;
    private String childHierarchyId;
    private BigDecimal amount;
    private WalletType fromWalletType;
    private WalletType toWalletType;
    private LocalDateTime transferDate;
    private String narration;

}
