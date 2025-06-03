package com.im.admin.service;

import com.im.admin.dto.*;

import java.util.List;

public interface TransactionService {

    String getOperatorDetailsByCardBIN(CardTypeRequestDto cardTypeRequestDto);

    TransactionSchemeDetailsResponseDto getTransactionSchemeDetails(TransactionSchemeDetailsRequestDto request);

    String processTransaction(TransactionSchemeDetailsRequestDto request);

    String updateTransactionStatus(UpdateTransactionStatusRequest request);

    List<TransactionSearchResponseDTO> getTransactions(
            String hierarchyId, String transactionId, String status, String walletType);

}

