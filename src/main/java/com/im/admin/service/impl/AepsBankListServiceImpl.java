package com.im.admin.service.impl;

import com.im.admin.response.FingpayBankApiResponse;
import com.im.admin.service.AepsBankListService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class AepsBankListServiceImpl implements AepsBankListService {
    ;
    @Autowired
    private RestTemplate restTemplate;

    private static final String BANK_API =
            "https://fingpayap.tapits.in/fpaepsservice/api/bankdata/bank/details";

    @Override
    public FingpayBankApiResponse getFullBankList() {
        FingpayBankApiResponse response =
                restTemplate.getForObject(BANK_API, FingpayBankApiResponse.class);

        if (response == null) {
            throw new RuntimeException("Null response from FingPay");
        }

        return response;  // FULL vendor JSON
    }
    }
