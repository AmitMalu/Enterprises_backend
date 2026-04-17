package com.im.admin.service;

import com.im.admin.request.AepsCashWithdrawalRequest;
import com.im.admin.response.AepsCashWithdrawalResponse;
import com.im.admin.response.CommonResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestHeader;

public interface AepsCashWithdrawalService {
    CommonResponse<AepsCashWithdrawalResponse> cashWithdrawal(AepsCashWithdrawalRequest req,  Double latitude,  Double longitude);

}

