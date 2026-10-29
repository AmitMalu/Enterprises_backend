package com.scs.admin.service.impl;


import com.scs.admin.dto.AbstractWalletAmountTable;
import com.scs.admin.response.WalletBalanceResponseDto;
import com.scs.admin.service.CommonService;
import com.scs.admin.util.CommonMethods;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@Slf4j
public class CommonServiceImpl implements CommonService {

    @Autowired
    private CommonMethods commonMethods;

    @Override
    public WalletBalanceResponseDto fetchWalletBalance(String hierarchyId) {
        WalletBalanceResponseDto response = new WalletBalanceResponseDto();

        // Fetch Prepaid Balance
        AbstractWalletAmountTable prepaidWallet = commonMethods.getWalletByType("PREPAID", hierarchyId);
        response.setPrepaidBalance(
                prepaidWallet != null ? prepaidWallet.getAvailableBalance() : BigDecimal.valueOf(0.00)
        );

        // Fetch Postpaid Balance
        AbstractWalletAmountTable postpaidWallet = commonMethods.getWalletByType("POSTPAID", hierarchyId);
        response.setPostpaidBalance(
                postpaidWallet != null ? postpaidWallet.getAvailableBalance() : BigDecimal.valueOf(0.00)
        );

        return response;
    }

}
