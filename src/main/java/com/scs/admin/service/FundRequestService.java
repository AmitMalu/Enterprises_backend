package com.scs.admin.service;

import com.scs.admin.dto.ChildUserDto;
import com.scs.admin.dto.FundRequestActionDto;
import com.scs.admin.dto.FundRequestDto;
import com.scs.admin.entities.FundRequest;

import java.util.List;

public interface FundRequestService {

    void raiseFundRequest(FundRequestDto fundRequestDto);

    // void takeActionOnFundRequest(FundRequestActionDto actionDto);

    List<FundRequest> fetchAllRecords();


}

