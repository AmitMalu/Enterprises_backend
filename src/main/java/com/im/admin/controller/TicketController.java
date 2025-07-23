package com.im.admin.controller;

import com.im.admin.dto.*;
import com.im.admin.entities.Ticket;
import com.im.admin.response.CommonResponse;
import com.im.admin.service.TicketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.fasterxml.jackson.databind.ObjectMapper;


import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/ticket")
public class TicketController {

    @Autowired
    private ObjectMapper objectMapper;


    @Autowired
    private TicketService ticketService;

    @PostMapping("/transaction-details")
    public ResponseEntity<?> getTransactionDetails(@RequestBody TransactionRequestDto requestDto) {
        Object result = ticketService.getTransactionDetails(
                requestDto.getTransactionId(),
                requestDto.getHierarchyId()
        );
        return ResponseEntity.ok(new CommonResponse<>(200, "TransactionDetails fetch successfully.",result));
    }


    @PostMapping(value = "/raise-ticket", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CommonResponse> raiseTicket(
            @RequestPart("data") String ticketJson,
            @RequestPart("screenshotImage") MultipartFile screenshotImage) throws IOException {

        RaiseTicketRequestDto request = objectMapper.readValue(ticketJson, RaiseTicketRequestDto.class);
        Ticket ticket = ticketService.saveTicket(request, screenshotImage);
        return ResponseEntity.ok(new CommonResponse<>(200, "Ticket raised successfully.",ticket));


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

    @GetMapping("/details/{ticketId}")
    public ResponseEntity<Ticket> getTicketDetails(@PathVariable Long ticketId) {
        Ticket ticket = ticketService.getTicketWithComments(ticketId);
        return ResponseEntity.ok(ticket);
    }

    @GetMapping("/issue-type-dropdown")
    public ResponseEntity<CommonResponse<List<TicketIssueTypeDto>>> getIssueTypeDropdown() {
        List<TicketIssueTypeDto> result = ticketService.getAllIssueTypes();
        return ResponseEntity.ok(new CommonResponse<>(200, "Issue types fetched", result));

    }

    @GetMapping
    public ResponseEntity<CommonResponse<Page<TicketWithTransactionDto>>> getAllTickets(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("id").ascending());

        Page<TicketWithTransactionDto> tickets = ticketService.getAllTickets(pageable);
        return ResponseEntity.ok(new CommonResponse<>(200, "Tickets fetched successfully.", tickets));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CommonResponse> getTicketById(@PathVariable Long id) {
        Ticket ticket = ticketService.getTicketById(id);
        return ResponseEntity.ok(new CommonResponse<>(200, "Success", ticket));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CommonResponse<Ticket>> updateTicketStatusAndRemark(
            @PathVariable Long id,
            @RequestBody TicketStatusUpdateDto dto) {

        Ticket updatedTicket = ticketService.updateStatusAndRemark(id, dto);
        return ResponseEntity.ok(new CommonResponse<>(200, "Ticket updated successfully", updatedTicket));
    }



}
