package com.scs.admin.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class HierarchyCountResponse {

    private String hierarchyId;
    private Map<String, Long> counts;
    private long totalCount;

}
