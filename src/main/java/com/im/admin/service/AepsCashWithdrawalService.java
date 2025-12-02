package com.im.admin.service;

import com.im.admin.request.AepsCashWithdrawalRequest;
import com.im.admin.response.AepsCashWithdrawalResponse;
import com.im.admin.response.CommonResponse;
import org.springframework.http.ResponseEntity;

public interface AepsCashWithdrawalService {
    CommonResponse<AepsCashWithdrawalResponse> cashWithdrawal(AepsCashWithdrawalRequest req);
}

