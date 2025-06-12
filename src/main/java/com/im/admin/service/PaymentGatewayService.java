package com.im.admin.service;

import com.im.admin.dto.*;
import com.im.admin.entities.CardTypeMaster;
import com.im.admin.entities.SchemeAllocation;
import com.im.admin.request.BulkUpdateTransactionStatusRequest;
import com.im.admin.request.CreateOrderRequest;
import com.im.admin.response.CommonResponse;
import com.im.admin.response.RunPaisaCallback;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;

import java.util.Map;

public interface PaymentGatewayService {

    String generateToken();

    CommonResponse<RetailerCommissionResponseDTO> fetchRetailerCommission(PaymentGatewayCardBinRequestDTO paymentGatewayCardBinRequestDTO);

    //String bulkUpdateTransactionStatus(BulkUpdateTransactionStatusRequest bulkRequest);

    //String updateTransactionStatus(UpdateTransactionStatusRequest request);

    ResponseEntity<?> createOrder(CreateOrderRequest request);

    ResponseEntity<?> orderStatus(String orderId);

    String handleCallback(Map<String, String> payload, HttpServletRequest request);

    void processCallback(RunPaisaCallback callback);


}
