package com.scs.admin.service;

import com.scs.admin.dto.RequestHeaderMetaDto;
import com.scs.admin.request.BillFetchRequest;
import com.scs.admin.request.BillPaymentRequest;
import com.scs.admin.request.BillerRequest;
import com.scs.admin.request.TransactionStatusRequest;

public interface BillAvenueService {

    Object fetchBillerInfobulk(BillerRequest request) throws Exception;


    Object fetchBillerInfo(BillerRequest request) throws Exception;

    Object fetchBill(BillFetchRequest request)throws Exception;

    Object doBillPayment(BillPaymentRequest request, RequestHeaderMetaDto requestHeaderMetaDtoMeta) throws Exception;

    Object fetchTransactionStatus(TransactionStatusRequest request)throws Exception ;
}
