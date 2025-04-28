package com.im.admin.service;

import com.im.admin.dto.FundRequestActionDto;
import com.im.admin.dto.FundRequestDto;

public interface FundRequestService {

    void raiseFundRequest(FundRequestDto fundRequestDto);

    void takeActionOnFundRequest(FundRequestActionDto actionDto);
}

