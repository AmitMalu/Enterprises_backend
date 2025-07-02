package com.im.admin.service.impl;


import com.im.admin.entities.FundTransfer;
import com.im.admin.repositories.FundTransferRepository;
import com.im.admin.request.FundTransferRequestDto;
import com.im.admin.response.FundTransferResponseDto;
import com.im.admin.service.FundTransferService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class FundTransferServiceImpl implements FundTransferService {

    @Autowired
    private FundTransferRepository fundTransferRepository;

    @Override
    @Transactional
    public FundTransferResponseDto transferFunds(FundTransferRequestDto requestDto) {
        FundTransfer fundTransfer = new FundTransfer();
        fundTransfer.setFromHierarchyId(requestDto.getParentHierarchyId());
        fundTransfer.setToHierarchyId(requestDto.getChildHierarchyId());
        fundTransfer.setAmount(requestDto.getAmount());
        fundTransfer.setFromWalletType(requestDto.getFromWalletType());
        fundTransfer.setToWalletType(requestDto.getToWalletType());
        fundTransfer.setNarration(requestDto.getNarration());

        FundTransfer saved = fundTransferRepository.save(fundTransfer);

        return new FundTransferResponseDto(
                saved.getId(),
                saved.getFromHierarchyId(),
                saved.getToHierarchyId(),
                saved.getAmount(),
                saved.getFromWalletType(),
                saved.getToWalletType(),
                saved.getTransferDate(),
                saved.getNarration()
        );
    }

}
