package com.im.admin.dto;

import com.im.admin.entities.enums.Status;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class FundReversalActionDto {

    private Long fundReversalRequestId;
    private Status status; // APPROVED or REJECTED
    private String remarks;
}

