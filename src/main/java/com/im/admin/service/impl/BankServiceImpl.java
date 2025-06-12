package com.im.admin.service.impl;

import com.im.admin.dto.BankRequestDto;
import com.im.admin.entities.BankMaster;
import com.im.admin.repositories.BankMasterRepository;
import com.im.admin.service.BankService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authorization.method.AuthorizeReturnObject;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class BankServiceImpl implements BankService {

    @Autowired
    private BankMasterRepository bankMasterRepository;

    @Override
    public BankMaster addBank(BankRequestDto bankRequestDto) {
        BankMaster bank = new BankMaster();
        bank.setBankCode(bankRequestDto.getBankCode());
        bank.setBankName(bankRequestDto.getBankName());
        bank.setIFSCCode(bankRequestDto.getIfscCode());
        bank.setImps(bankRequestDto.getImps());
        bank.setNeft(bankRequestDto.getNeft());
        bank.setRtgs(bankRequestDto.getRtgs());
        bank.setActiveStatus(bankRequestDto.getActiveStatus());
        bank.setCreatedBy(bankRequestDto.getCreatedBy());
//        bank.setIsBlocked(false); // default
        // Set others if available like logo, maxAccountNoLength, verification
        return bankMasterRepository.save(bank);
    }

    @Override
    public List<BankMaster> getAllBanks() {
        return bankMasterRepository.findAll();
    }
}
