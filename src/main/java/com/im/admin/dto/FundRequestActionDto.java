package com.im.admin.dto;

import com.im.admin.entities.enums.Status;
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

}