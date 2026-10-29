package com.scs.admin.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ScsBankMasterResponseDTO {

    private Long id;
    private String bankName;
    private String accountNumber;
    private String ifscCode;
    private Boolean charges;
    private String chargesType;
    private Boolean isActive;
    private Boolean isBlocked;
    private String createdBy;
    private String editedBy;
    private LocalDateTime createdOn;
    private LocalDateTime editedOn;
    private String whitelabelId;

}
