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
public class VendorCredentialsResponseDTO {
    private Long id;

    private String vendorId;
    private String vendorName;

    private Short productId;
    private String productName;

    private Boolean isWhiteLabelAdmin;
    private String whiteLabelAdminId;
    private String whiteLabelAdminName;

    private Boolean isActive;
    private String baseUrlUat;
    private String baseUrlProd;
    private String tokenUrlUat;
    private String tokenUrlProd;
    private String clientId;
    private String clientSecret;
    private String saltKey;
    private String username;
    private String password;
    private String userField1;
    private String userField2;
    private String userField3;
    private LocalDateTime createdOn;
    private String createdBy;
    private LocalDateTime editedOn;
    private String editedBy;
}


