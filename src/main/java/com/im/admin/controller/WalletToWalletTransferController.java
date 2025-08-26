package com.im.admin.controller;

import com.im.admin.dto.WalletToWalletTransferDto;
import com.im.admin.response.CommonResponse;
import com.im.admin.service.WalletToWalletTransferService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class WalletToWalletTransferController {

    @Autowired
    private WalletToWalletTransferService walletTransferService;

    @PostMapping("/transfer")
    public ResponseEntity<CommonResponse<String>> transferAmount(@RequestBody WalletToWalletTransferDto request) {
        log.info("Received wallet-to-wallet transfer request: {}", request);

        try {
            walletTransferService.transferAmountWalletToWallet(request);
            log.info("Transfer successful from wallet {} to wallet {} for amount {}",
                    request.getWalletTypeFrom(), request.getWalletTypeTo(), request.getTransferAmount());

            return ResponseEntity.ok(new CommonResponse<>(200, "Transfer successful", null));
        } catch (IllegalArgumentException e) {
            log.warn("Transfer failed due to invalid input: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new CommonResponse<>(400, "Transfer failed: " + e.getMessage(), null));
        } catch (Exception e) {
            log.error("Unexpected error during transfer: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new CommonResponse<>(500, "Transfer failed: " + e.getMessage(), null));
        }
    }


}
