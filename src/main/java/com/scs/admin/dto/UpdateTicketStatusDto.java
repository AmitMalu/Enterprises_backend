package com.scs.admin.dto;

import lombok.Data;

@Data
public class UpdateTicketStatusDto {
    private Long ticketId;
    private String newStatus; // e.g., RESOLVED
}


