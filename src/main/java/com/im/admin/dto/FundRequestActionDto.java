package com.im.admin.dto;

import com.im.admin.entities.enums.ConstantStatus;
import lombok.*;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FundRequestActionDto {

    private Long fundRequestId;
    private ConstantStatus status; // APPROVED or REJECTED
    private String remarks;

}