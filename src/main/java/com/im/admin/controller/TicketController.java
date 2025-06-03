package com.im.admin.controller;

import com.im.admin.dto.*;
import com.im.admin.entities.Ticket;
import com.im.admin.response.CommonResponse;
import com.im.admin.service.TicketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ticket")
public class TicketController {

    @Autowired
    private TicketService ticketService;

    @PostMapping("/transaction-details")
    public ResponseEntity<?> getTransactionDetails(@RequestBody TransactionRequestDto requestDto) {
        Object result = ticketService.getTransactionDetails(
                requestDto.getTransactionId(),
                requestDto.getHierarchyId(),
                requestDto.getWalletType()
        );
        return ResponseEntity.ok(result);
    }

    @PostMapping("/raise-ticket")
    public ResponseEntity<CustomResponseDTO> raiseTicket(@RequestBody RaiseTicketRequestDto request) {
        ticketService.saveTicket(request);
        return ResponseEntity.ok(new CustomResponseDTO(200, "Ticket raised successfully."));
    }

    @PostMapping("/comment")
    public ResponseEntity<CustomResponseDTO> addComment(@RequestBody AddTicketCommentDto dto) {
        ticketService.addComment(dto);
        return ResponseEntity.ok(new CustomResponseDTO(200, "Comment added."));
    }

    @PostMapping("/status")
    public ResponseEntity<CustomResponseDTO> updateStatus(@RequestBody UpdateTicketStatusDto dto) {
        ticketService.updateStatus(dto);
        return ResponseEntity.ok(new CustomResponseDTO(200, "Ticket status updated."));
    }

    @GetMapping("/{ticketId}")
    public ResponseEntity<Ticket> getTicketDetails(@PathVariable Long ticketId) {
        Ticket ticket = ticketService.getTicketWithComments(ticketId);
        return ResponseEntity.ok(ticket);
    }
}
