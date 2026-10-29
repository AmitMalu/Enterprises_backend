package com.scs.admin.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author SHUBHAM KHOPADE
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class HierarchyRegistrationRequest {

    private String hierarchyId;

    private String hierarchyType;

    private String parentHierarchyId;
}
