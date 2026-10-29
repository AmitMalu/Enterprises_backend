package com.scs.admin.service;

import com.scs.admin.request.PayoutResponseDto;
import com.scs.admin.request.SendPayoutRequest;
import com.scs.admin.response.CommonResponse;

public interface MoveToBankService {

    CommonResponse<PayoutResponseDto> sendPayoutRequest(SendPayoutRequest sendPayoutRequest, Double latitude, Double longitude);

}
