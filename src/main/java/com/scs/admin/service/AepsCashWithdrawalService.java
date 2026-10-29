package com.scs.admin.service;

import com.scs.admin.request.AepsCashWithdrawalRequest;
import com.scs.admin.response.AepsCashWithdrawalResponse;
import com.scs.admin.response.CommonResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestHeader;

public interface AepsCashWithdrawalService {
    CommonResponse<AepsCashWithdrawalResponse> cashWithdrawal(AepsCashWithdrawalRequest req,  Double latitude,  Double longitude);

}

