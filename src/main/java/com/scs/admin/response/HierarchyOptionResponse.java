package com.scs.admin.response;

/**
 * @author SHUBHAM KHOPADE
 */
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HierarchyOptionResponse {

    private String hierarchyId;

    private String hierarchyCode;
}
