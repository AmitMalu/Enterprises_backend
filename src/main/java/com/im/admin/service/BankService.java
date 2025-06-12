package com.im.admin.service;

import com.im.admin.dto.BankRequestDto;
import com.im.admin.entities.BankMaster;
import org.springframework.stereotype.Service;

import java.util.List;


public interface BankService {
    BankMaster addBank(BankRequestDto bankRequestDto);

    List<BankMaster> getAllBanks();
}
