package com.im.admin.service;

import com.im.admin.dto.CardTypeRequestDto;
import com.im.admin.dto.TransactionSchemeDetailsRequestDto;
import com.im.admin.dto.TransactionSchemeDetailsResponseDto;

public interface TransactionService {

    String getOperatorDetailsByCardBIN(CardTypeRequestDto cardTypeRequestDto);

    TransactionSchemeDetailsResponseDto getTransactionSchemeDetails(TransactionSchemeDetailsRequestDto request);

    String processTransaction(TransactionSchemeDetailsRequestDto request);
}

