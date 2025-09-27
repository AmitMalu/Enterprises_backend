package com.im.admin.service;

import com.im.admin.dto.*;
import com.im.admin.entities.Ticket;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.Map;

public interface TicketService {
    Object getTransactionDetails(String transactionId, String hierarchyId);

   

    void addComment(AddTicketCommentDto dto);

    void updateStatus(UpdateTicketStatusDto dto);

    Ticket getTicketWithComments(Long ticketId);

    Ticket saveTicket(RaiseTicketRequestDto request, MultipartFile screenshotImage);

    List<TicketIssueTypeDto> getAllIssueTypes();

    Page<TicketWithTransactionDto> getAllTickets(Map<String, Object> filters, Pageable pageable);


    Ticket getTicketById(Long id);

    Ticket updateStatusAndRemark(Long id, TicketStatusUpdateDto dto);

    void deleteTicketById(Long id);
}
