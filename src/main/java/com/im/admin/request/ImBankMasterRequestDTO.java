package com.im.admin.request;


import jakarta.persistence.Column;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ImBankMasterRequestDTO {

    private String bankName;
    private String accountNumber;
    private String ifscCode;
    private Boolean charges;
    private String chargesType;
    private Boolean isActive;
    private Boolean isBlocked;
    private Boolean isWhiteLabelAdmin;
    //private String whiteLabelAdminId;

    private String createdBy;
    private String editedBy;
    private String whitelabelId;

}
