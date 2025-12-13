package com.im.admin.controller;

import com.im.admin.util.BillAvenueCryptoUtil;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/billavenue")
public class BillAvenueEncryptController {

    private static final String WORKING_KEY = "063F0699C1D46414EA1E92D0E6BDCF99";

    @PostMapping("/encrypt")
    public String encrypt(@RequestBody String json) throws Exception {
        return BillAvenueCryptoUtil.encrypt(json, WORKING_KEY);
    }

    @PostMapping("/decrypt")
    public String decrypt(@RequestBody String encHex) throws Exception {
        return BillAvenueCryptoUtil.decrypt(encHex, WORKING_KEY);
    }
}
