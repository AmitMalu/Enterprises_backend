package com.im.admin.service;

import com.im.admin.dto.*;
import com.im.admin.entities.CardTypeMaster;
import com.im.admin.entities.SchemeAllocation;
import com.im.admin.request.BulkUpdateTransactionStatusRequest;
import com.im.admin.request.CreateOrderRequest;
import com.im.admin.response.CommonResponse;
import org.springframework.http.ResponseEntity;

public interface PaymentGatewayService {

    String generateToken();

    CommonResponse<RetailerCommissionResponseDTO> fetchRetailerCommission(PaymentGatewayCardBinRequestDTO paymentGatewayCardBinRequestDTO);

    //String bulkUpdateTransactionStatus(BulkUpdateTransactionStatusRequest bulkRequest);

    //String updateTransactionStatus(UpdateTransactionStatusRequest request);

    ResponseEntity<?> createOrder(CreateOrderRequest request);

}
