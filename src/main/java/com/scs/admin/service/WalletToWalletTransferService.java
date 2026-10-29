package com.scs.admin.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.scs.admin.dto.RequestHeaderMetaDto;
import com.scs.admin.dto.WalletToWalletTransferDto;

public interface WalletToWalletTransferService {

    void transferAmountWalletToWallet(WalletToWalletTransferDto request, RequestHeaderMetaDto requestHeaderMetaDtoMeta) throws JsonProcessingException;

}
