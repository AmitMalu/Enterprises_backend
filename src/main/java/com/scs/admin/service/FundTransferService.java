package com.scs.admin.service;

import com.scs.admin.dto.RequestHeaderMetaDto;
import com.scs.admin.request.FundTransferRequestDto;
import com.scs.admin.response.FundTransferResponseDto;

public interface FundTransferService {

    FundTransferResponseDto transferFunds(FundTransferRequestDto requestDto, RequestHeaderMetaDto requestHeaderMetaDtoMeta);

}
