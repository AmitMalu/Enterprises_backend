package com.im.admin.controller;

import com.im.admin.dto.WalletToWalletTransferDto;
import com.im.admin.service.WalletToWalletTransferService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/wallet-to-wallet")
@RequiredArgsConstructor
@Validated
public class WalletToWalletTransferController {

    @Autowired
    private WalletToWalletTransferService walletTransferService;

    @PostMapping("/transfer")
    public ResponseEntity<String> transferAmount(@RequestBody WalletToWalletTransferDto request) {
        try {
            walletTransferService.transferAmountWalletToWallet(request);
            return ResponseEntity.ok("Transfer successful");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Transfer failed: " + e.getMessage());
        }
    }

}
