package com.im.admin.response;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GetAllSchemeResponse {

    private Long id;
    private String schemeName;
    private String schemeCode;
    private String schemeCombinationHashCode;
    private String productId;
    private String productName;
    private String subProductId;
    private String subProductName;
    private String operatorId;
    private String operatorName;
    private String vendorId;
    private String vendorName;
    private String schemeHierarchyName;
    private String schemeHierarchySelectionId;
    private LocalDateTime createdOn;
    private String createdBy;
    private LocalDateTime editedOn;
    private String editedBy;
}
