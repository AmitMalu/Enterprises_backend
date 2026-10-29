package com.scs.admin.controller;

import com.scs.admin.dto.*;
import com.scs.admin.entities.Ticket;
import com.scs.admin.response.CommonResponse;
import com.scs.admin.service.TicketService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.fasterxml.jackson.databind.ObjectMapper;


import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@Slf4j
@RequestMapping("/api/ticket")
public class TicketController {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TicketService ticketService;


    @PostMapping("/transaction-details")
    public ResponseEntity<?> getTransactionDetails(@RequestBody TransactionRequestDto requestDto) {
        log.info("Fetching transaction details for transactionId: {}, hierarchyId: {}",
                requestDto.getTransactionId(), requestDto.getHierarchyId());

        try {
            Object result = ticketService.getTransactionDetails(
                    requestDto.getTransactionId(),
                    requestDto.getHierarchyId()
            );
            log.info("Transaction details fetched successfully");
            return ResponseEntity.ok(new CommonResponse<>(200, "TransactionDetails fetch successfully.", result));
        } catch (Exception e) {
            log.error("Error fetching transaction details", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new CommonResponse<>(500, "Failed to fetch transaction details", null));
        }
    }

    @PostMapping(value = "/raise-ticket", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CommonResponse> raiseTicket(
            @RequestPart("data") String ticketJson,
            @RequestPart("screenshotImage") MultipartFile screenshotImage) throws IOException {

        log.info("Raising ticket with screenshot");

        try {
            RaiseTicketRequestDto request = objectMapper.readValue(ticketJson, RaiseTicketRequestDto.class);
            log.debug("Parsed ticket request: {}", request);

            Ticket ticket = ticketService.saveTicket(request, screenshotImage);
            log.info("Ticket raised successfully. Ticket ID: {}", ticket.getId());

            return ResponseEntity.ok(new CommonResponse<>(200, "Ticket raised successfully.", ticket));
        } catch (Exception e) {
            log.error("Error raising ticket", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new CommonResponse<>(500, "Failed to raise ticket", null));
        }
    }

    @PostMapping("/comment")
    public ResponseEntity<CustomResponseDTO> addComment(@RequestBody AddTicketCommentDto dto) {
        log.info("Adding comment to ticket ID: {}", dto.getTicketId());

        try {
            ticketService.addComment(dto);
            log.info("Comment added successfully");
            return ResponseEntity.ok(new CustomResponseDTO(200, "Comment added."));
        } catch (Exception e) {
            log.error("Error adding comment", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new CustomResponseDTO(500, "Failed to add comment"));
        }
    }

    @PostMapping("/status")
    public ResponseEntity<CustomResponseDTO> updateStatus(@RequestBody UpdateTicketStatusDto dto) {
        log.info("Updating status for ticket ID: {} to {}", dto.getTicketId(), dto.getNewStatus());

        try {
            ticketService.updateStatus(dto);
            log.info("Ticket status updated successfully");
            return ResponseEntity.ok(new CustomResponseDTO(200, "Ticket status updated."));
        } catch (Exception e) {
            log.error("Error updating ticket status", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new CustomResponseDTO(500, "Failed to update ticket status"));
        }
    }

    @GetMapping("/details/{ticketId}")
    public ResponseEntity<Ticket> getTicketDetails(@PathVariable Long ticketId) {
        log.info("Fetching ticket details for ticket ID: {}", ticketId);

        try {
            Ticket ticket = ticketService.getTicketWithComments(ticketId);
            log.info("Ticket details fetched successfully");
            return ResponseEntity.ok(ticket);
        } catch (Exception e) {
            log.error("Error fetching ticket details", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/issue-type-dropdown")
    public ResponseEntity<CommonResponse<List<TicketIssueTypeDto>>> getIssueTypeDropdown() {
        log.info("Fetching issue type dropdown");

        try {
            List<TicketIssueTypeDto> result = ticketService.getAllIssueTypes();
            log.info("Issue types fetched successfully. Count: {}", result.size());
            return ResponseEntity.ok(new CommonResponse<>(200, "Issue types fetched", result));
        } catch (Exception e) {
            log.error("Error fetching issue types", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new CommonResponse<>(500, "Failed to fetch issue types", null));
        }
    }

    @GetMapping
    public ResponseEntity<CommonResponse<Page<TicketWithTransactionDto>>> getAllTickets(
            @RequestParam Map<String, String> params,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {  // Spring auto-binds query params into this DTO

        log.info("Fetching tickets with params: {}, Page: {}, Size: {}", params, page, size);

        try {
            Map<String, Object> filter = new HashMap<>(params);
            Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
            Page<TicketWithTransactionDto> tickets = ticketService.getAllTickets(filter, pageable);
            return ResponseEntity.ok(new CommonResponse<>(200, "Tickets fetched successfully.", tickets));
        } catch (Exception e) {
            log.error("Error fetching tickets", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new CommonResponse<>(500, "Failed to fetch tickets", null));
        }
    }


    @GetMapping("/{id}")
    public ResponseEntity<CommonResponse> getTicketById(@PathVariable Long id) {
        log.info("Fetching ticket by ID: {}", id);

        try {
            Ticket ticket = ticketService.getTicketById(id);
            log.info("Ticket fetched successfully for ID: {}", id);
            return ResponseEntity.ok(new CommonResponse<>(200, "Success", ticket));
        } catch (Exception e) {
            log.error("Error fetching ticket by ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new CommonResponse<>(500, "Failed to fetch ticket", null));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<CommonResponse<Ticket>> updateTicketStatusAndRemark(
            @PathVariable Long id,
            @RequestBody TicketStatusUpdateDto dto) {

        log.info("Updating ticket ID: {} with status: {} and remark: {}", id, dto.getStatus(), dto.getRemark());

        try {
            Ticket updatedTicket = ticketService.updateStatusAndRemark(id, dto);
            log.info("Ticket updated successfully for ID: {}", id);
            return ResponseEntity.ok(new CommonResponse<>(200, "Ticket updated successfully", updatedTicket));
        } catch (Exception e) {
            log.error("Error updating ticket ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new CommonResponse<>(500, "Failed to update ticket", null));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<CommonResponse<String>> deleteTicket(@PathVariable Long id) {
        ticketService.deleteTicketById(id);
        return ResponseEntity.ok(new CommonResponse<>(200, "Ticket deleted successfully", "Deleted ID: " + id));
    }

}
