package com.scs.admin.request;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SchemeHierarchySelectionRequest {

    private String schemeHierarchySelectionName;
    private List<String> schemeHierarchyCombination;
    private String createdBy;
    private String editedBy;
}
