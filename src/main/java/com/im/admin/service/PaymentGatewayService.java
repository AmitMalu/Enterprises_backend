package com.im.admin.service;

import com.im.admin.dto.FetchSubProductFromRetailerRequest;
import com.im.admin.dto.PaymentGatewayCardBinRequestDTO;
import com.im.admin.dto.RetailerCommissionResponseDTO;
import com.im.admin.dto.UpdateTransactionStatusRequest;
import com.im.admin.entities.CardTypeMaster;
import com.im.admin.entities.SchemeAllocation;
import com.im.admin.request.BulkUpdateTransactionStatusRequest;
import com.im.admin.response.CommonResponse;

public interface PaymentGatewayService {

    CommonResponse<RetailerCommissionResponseDTO> fetchRetailerCommission(PaymentGatewayCardBinRequestDTO paymentGatewayCardBinRequestDTO);

    //String bulkUpdateTransactionStatus(BulkUpdateTransactionStatusRequest bulkRequest);

    //String updateTransactionStatus(UpdateTransactionStatusRequest request);



}
