package com.scs.admin.dto;

import com.scs.admin.entities.enums.Status;
import lombok.*;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FundRequestActionDto {

    private String approveOrRejectHierarchyId;
    private Long fundRequestId;
    private Status status; // APPROVED or REJECTED
    private String remarks;
    private String schemeCode;
    private String subProductId;

}