package com.im.admin.dto;

import lombok.Data;

@Data
public class KYCEditRequest {
    private String hierarchyId;
    private String type;        // RT / DT / PT / SD / WL / ET
    private String category;    // AADHAAR / PAN / SHOP / BUSINESS
    private String subCategory; // for Aadhaar: FRONT / BACK
    private boolean verified;
}
