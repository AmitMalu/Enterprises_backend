package com.scs.admin.service;

import com.scs.admin.dto.*;
import com.scs.admin.response.BulkTransactionStatusResponse;
import com.scs.admin.response.CommonResponse;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface TransactionEngineService {

    CommonResponse<CardBinDetailsResponseDto> getDetailsByCardBINNumber(CardTypeRequestDto cardTypeRequestDto);

    TransactionSchemeDetailsResponseDto getTransactionSchemeDetails(TransactionSchemeDetailsRequestDto request);

    String processTransaction(TransactionSchemeDetailsRequestDto request, String vendorCallbackStatus);

    String updateTransactionStatus(UpdateTransactionStatusRequest request);

    CommonResponse<BulkTransactionStatusResponse> updatePendingTransactionStatus(UpdateTransactionStatusRequest request);

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

}