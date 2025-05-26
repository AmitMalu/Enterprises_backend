package com.im.admin.service.impl;


import com.im.admin.entities.CardTypeMaster;
import com.im.admin.repositories.CardTypeMasterRepository;
import com.im.admin.service.PaymentGatewayService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PaymentGatewayServiceImpl implements PaymentGatewayService {

    @Autowired
    private CardTypeMasterRepository cardTypeMasterRepository;

    @Override
    public CardTypeMaster fetchCardTypeMaster(String cardBinNo) {
        return cardTypeMasterRepository.findByCardBinNo(cardBinNo).orElse(null);
    }
}
