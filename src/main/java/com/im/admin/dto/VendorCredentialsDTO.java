package com.im.admin.dto;

import com.im.admin.entities.product.Products;
import com.im.admin.entities.vendor.Vendors;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class VendorCredentialsDTO {

    private Long id;
    private Vendors vendorId;
    private Products productId;
    private String baseUrlUat;
    private String baseUrlProd;
    private String clientId;
    private String saltKey;
    private String clientSecret;
    private String username;
    private String password;
    private Boolean isActive;
    private Boolean isWhiteLabelAdmin;
    private String whiteLabelAdminId;
    private String userField1;
    private String userField2;
    private String userField3;
    private String createdBy;
    private String editedBy;

}

