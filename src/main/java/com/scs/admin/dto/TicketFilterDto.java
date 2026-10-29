package com.scs.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TicketFilterDto {

    private String transactionId;
    private String issueType;
    private String description;
    private String email;
    private String mobileNumber;
    private String subProductName;
    private String status;
    private String raisedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String tranStatus;

}
