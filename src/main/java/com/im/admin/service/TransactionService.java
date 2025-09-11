package com.im.admin.service;

import com.im.admin.dto.*;
import com.im.admin.request.BulkUpdateTransactionStatusRequest;
import com.im.admin.response.CommonResponse;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface TransactionService {

    String getOperatorDetailsByCardBIN(CardTypeRequestDto cardTypeRequestDto);

    CommonResponse<CardBinDetailsResponseDto> getDetailsByCardBINNumber(CardTypeRequestDto cardTypeRequestDto);

    TransactionSchemeDetailsResponseDto getTransactionSchemeDetails(TransactionSchemeDetailsRequestDto request);

    String processTransaction(TransactionSchemeDetailsRequestDto request, String vendorCallbackStatus);

    String updateTransactionStatus(UpdateTransactionStatusRequest request);


    //String bulkUpdateTransactionStatus(BulkUpdateTransactionStatusRequest bulkRequest);

    List<Map<String, Object>> getSettlementTransactionsByFilter(
            LocalDate fromDate,
            LocalDate toDate,
            String subProductName,
            String settlementType,
            String walletType
    );

    CommonResponse<List<Map<String, String>>> bulkUpdateSettlementStatus(
            List<String> transactionIds,
            String walletType,
            String newStatus);

   // void enqueueTransactions(List<String> transactionIds, String walletType, String newStatus);

   // CommonResponse<String> bulkUpdateSettlementStatus(List<String> transactionIds, String walletType, String newStatus);


    }