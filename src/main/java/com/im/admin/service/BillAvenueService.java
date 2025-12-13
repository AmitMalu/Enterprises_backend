package com.im.admin.service;

import com.im.admin.request.BillFetchRequest;
import com.im.admin.request.BillPaymentRequest;
import com.im.admin.request.BillerRequest;

public interface BillAvenueService {

    Object fetchBillerInfo(BillerRequest request) throws Exception;

    Object fetchBill(BillFetchRequest request)throws Exception;

    Object doBillPayment(BillPaymentRequest request) throws Exception;
}
