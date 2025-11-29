package com.im.admin.service;

import com.im.admin.request.AepsCashWithdrawalRequest;
import com.im.admin.response.AepsCashWithdrawalResponse;
import org.springframework.http.ResponseEntity;

public interface AepsCashWithdrawalService {
    ResponseEntity<AepsCashWithdrawalResponse> cashWithdrawal(AepsCashWithdrawalRequest req);
}
