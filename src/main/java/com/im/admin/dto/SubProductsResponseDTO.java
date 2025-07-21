package com.im.admin.dto;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SubProductsResponseDTO {

    private String id;
    private String subProductTitle;
    private String subProductCode;
    private String walletType;
    private Boolean isSettlement;
    private String settlementTime;
    private Boolean isActive;
    private Boolean isVisible;
    private Boolean isBlocked;
    private Boolean isWhiteLabelAdmin;
    private String whiteLabelAdminId;
    private String userField1;
    private String userField2;
    private String userField3;
    private String createdBy;
    private String editedBy;
    private LocalDateTime createdOn;
    private LocalDateTime editedOn;

    private Short productId;
    private String productCode;
    private String productName;

}

