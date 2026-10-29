package com.scs.admin.service;

import com.scs.admin.request.ReportReceiptRequestDto;

public interface ReportReceiptService {
    Object getReceipt(ReportReceiptRequestDto request);
}
