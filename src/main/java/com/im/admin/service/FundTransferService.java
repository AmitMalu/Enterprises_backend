package com.im.admin.service;

import com.im.admin.dto.RequestHeaderMetaDto;
import com.im.admin.request.FundTransferRequestDto;
import com.im.admin.response.FundTransferResponseDto;

public interface FundTransferService {

    FundTransferResponseDto transferFunds(FundTransferRequestDto requestDto, RequestHeaderMetaDto requestHeaderMetaDtoMeta);

}
