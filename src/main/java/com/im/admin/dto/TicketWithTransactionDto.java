package com.im.admin.dto;

import com.im.admin.entities.TicketComment;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class TicketWithTransactionDto {
    private Long id;
    private String transactionId;
    private String issueType;
    private String description;
    private String email;
    private String mobileNumber;
    private String subProductName;
    private String status;
    private LocalDateTime createdAt;
    private String screenshotImage;
    private String raisedBy;
    private LocalDateTime updatedAt;
    private List<TicketComment> comments;

    // Additional transaction fields
    private String tranStatus;
    private BigDecimal balBeforeTran;
    private BigDecimal balAfterTran;
    private LocalDate dateOfTransaction;
    private BigDecimal amount;
    private String service;
    private String actionOnBalanceAmount;
}
