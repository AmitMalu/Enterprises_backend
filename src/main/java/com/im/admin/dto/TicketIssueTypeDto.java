package com.im.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TicketIssueTypeDto {

    private Long id;
    private String issueType;
}
