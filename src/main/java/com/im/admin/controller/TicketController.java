package com.im.admin.controller;

import com.im.admin.dto.TransactionRequestDto;
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

}
