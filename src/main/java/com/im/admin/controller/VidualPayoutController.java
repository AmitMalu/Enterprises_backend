package com.im.admin.controller;

import com.im.admin.request.EncryptedRequest;
import com.im.admin.util.VidualPayCryptoUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author SHUBHAM KHOPADE
 */

@RestController
@Slf4j
@RequestMapping("/api/vidual-pay")
@RequiredArgsConstructor
public class VidualPayoutController {

    private final VidualPayCryptoUtil cryptoUtil;


    @PostMapping("/encrypt")
    public ResponseEntity<String> encrypt(
            @RequestBody String request
    ) {

        String encrypted =
                cryptoUtil.encrypt(
                        request
                );

        return ResponseEntity.ok(
                encrypted
        );
    }


    @PostMapping("/decrypt")
    public ResponseEntity<String> decrypt(
            @RequestBody EncryptedRequest request
    ) {

        String decrypted =
                VidualPayCryptoUtil.decrypt(
                        request.getData()
                );

        return ResponseEntity.ok(
                decrypted
        );
    }

}
