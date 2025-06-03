package com.im.admin.dto;

import lombok.Data;

@Data
public class AddTicketCommentDto {
    private Long ticketId;
    private String commentText;
    private String addedBy; // USER / ADMIN
}
