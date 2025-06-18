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
    private String productName;
    private String subProductName;
    private LocalDateTime createdOn;
    private String createdBy;
    private LocalDateTime editedOn;
    private String editedBy;
}
