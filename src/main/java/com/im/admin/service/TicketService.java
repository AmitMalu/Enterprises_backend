package com.im.admin.service;

import com.im.admin.dto.AddTicketCommentDto;
import com.im.admin.dto.RaiseTicketRequestDto;
import com.im.admin.dto.UpdateTicketStatusDto;
import com.im.admin.entities.Ticket;

public interface TicketService {
    Object getTransactionDetails(String transactionId, String hierarchyId, String walletType);

    void saveTicket(RaiseTicketRequestDto request);

    void addComment(AddTicketCommentDto dto);

    void updateStatus(UpdateTicketStatusDto dto);

    Ticket getTicketWithComments(Long ticketId);
}
