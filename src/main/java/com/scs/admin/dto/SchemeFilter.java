package com.scs.admin.dto;

import lombok.*;

/**
 * @author SHUBHAM KHOPADE
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SchemeFilter {

    private Long id;

    private String schemeName;

    private String schemeCode;

    private String schemeCombinationHashCode;

    private String productId;

    private String productName;

    private String subProductId;

    private String subProductName;

    private String vendorId;

    private String vendorName;

    private String schemeHierarchyName;

    private String schemeHierarchySelectionId;

    private String createdOn;

    private String createdBy;

    private String editedOn;

    private String editedBy;
}
