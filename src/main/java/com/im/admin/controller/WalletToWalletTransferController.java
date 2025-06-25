package com.im.admin.controller;

import com.im.admin.dto.WalletToWalletTransferDto;
import com.im.admin.response.CommonResponse;
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
    public ResponseEntity<CommonResponse<String>> transferAmount(@RequestBody WalletToWalletTransferDto request) {
        try {
            walletTransferService.transferAmountWalletToWallet(request);
            return ResponseEntity.ok(new CommonResponse<>(200, "Transfer successful", null));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(new CommonResponse<>(400, "Transfer failed: " + e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new CommonResponse<>(500, "Transfer failed: " + e.getMessage(), null));
        }
    }


}
