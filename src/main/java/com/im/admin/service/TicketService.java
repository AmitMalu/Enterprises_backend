package com.im.admin.service;

import com.im.admin.dto.AddTicketCommentDto;
import com.im.admin.dto.RaiseTicketRequestDto;
import com.im.admin.dto.UpdateTicketStatusDto;
import com.im.admin.entities.Ticket;
import org.springframework.web.multipart.MultipartFile;

public interface TicketService {
    Object getTransactionDetails(String transactionId, String hierarchyId);

   

    void addComment(AddTicketCommentDto dto);

    void updateStatus(UpdateTicketStatusDto dto);

    Ticket getTicketWithComments(Long ticketId);

    Ticket saveTicket(RaiseTicketRequestDto request, MultipartFile screenshotImage);

}
