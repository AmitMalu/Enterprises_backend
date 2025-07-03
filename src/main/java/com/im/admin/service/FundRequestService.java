package com.im.admin.service;

import com.im.admin.dto.ChildUserDto;
import com.im.admin.dto.FundRequestActionDto;
import com.im.admin.dto.FundRequestDto;
import com.im.admin.entities.FundRequest;

import java.util.List;

public interface FundRequestService {

    void raiseFundRequest(FundRequestDto fundRequestDto);

   // void takeActionOnFundRequest(FundRequestActionDto actionDto);

    List<FundRequest> fetchAllRecords();

    List<ChildUserDto> getChildUsers(String hierarchyId);

//    List<?> getChildUsers(String hierarchyId);
}

