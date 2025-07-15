package com.im.admin.service;

import com.im.admin.dto.AddTicketCommentDto;
import com.im.admin.dto.RaiseTicketRequestDto;
import com.im.admin.dto.TicketIssueTypeDto;
import com.im.admin.dto.UpdateTicketStatusDto;
import com.im.admin.entities.Ticket;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

public interface TicketService {
    Object getTransactionDetails(String transactionId, String hierarchyId);

   

    void addComment(AddTicketCommentDto dto);

    void updateStatus(UpdateTicketStatusDto dto);

    Ticket getTicketWithComments(Long ticketId);

    Ticket saveTicket(RaiseTicketRequestDto request, MultipartFile screenshotImage);

    List<TicketIssueTypeDto> getAllIssueTypes();



}
