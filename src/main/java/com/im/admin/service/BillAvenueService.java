package com.im.admin.service;

import com.im.admin.request.BillFetchRequest;
import com.im.admin.request.BillPaymentRequest;
import com.im.admin.request.BillerRequest;
import com.im.admin.request.TransactionStatusRequest;

public interface BillAvenueService {

    Object fetchBillerInfobulk(BillerRequest request) throws Exception;


    Object fetchBillerInfo(BillerRequest request) throws Exception;

    Object fetchBill(BillFetchRequest request)throws Exception;

    Object doBillPayment(BillPaymentRequest request, Double latitude, Double longitude) throws Exception;

    Object fetchTransactionStatus(TransactionStatusRequest request)throws Exception ;
}
