package com.im.admin.dto;

import jakarta.persistence.Column;
import lombok.Data;

@Data
public class RaiseTicketRequestDto {
    private String transactionId;
    private String issueType;
    private String description;
    private String email;
    private String mobileNumber;
    private String subProductName;

}
