package com.im.admin.request;


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
    private List<String> hierarchyNames;
    private String createdBy;
}
