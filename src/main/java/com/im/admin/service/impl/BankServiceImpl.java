package com.im.admin.service.impl;

import com.im.admin.dto.BankRequestDto;
import com.im.admin.entities.BankMaster;
import com.im.admin.repositories.BankMasterRepository;
import com.im.admin.service.BankService;
import com.im.admin.spec.BankSpecification;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.authorization.method.AuthorizeReturnObject;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class BankServiceImpl implements BankService {

    @Autowired
    private BankMasterRepository bankMasterRepository;

    @Override
    public BankMaster addBank(BankRequestDto bankRequestDto) {
        BankMaster bank = new BankMaster();
        bank.setBankCode(bankRequestDto.getBankCode());
        bank.setBankName(bankRequestDto.getBankName());
        bank.setIfscCode(bankRequestDto.getIfscCode());
        bank.setImps(bankRequestDto.getImps());
        bank.setNeft(bankRequestDto.getNeft());
        bank.setRtgs(bankRequestDto.getRtgs());
//        bank.setActiveStatus(bankRequestDto.getActiveStatus());
        bank.setCreatedBy(bankRequestDto.getCreatedBy());
//        bank.setIsBlocked(false); // default
        // Set others if available like logo, maxAccountNoLength, verification
        return bankMasterRepository.save(bank);
    }

    @Override
    public Page<BankMaster> getAllBanks(Pageable pageable, String id, String bankCode, String bankName,
                                        String ifscCode, String imps, String neft,
                                        String rtgs, String activeStatus, String createdBy,
                                        String editedBy, Boolean isBlocked, LocalDate createdOn,
                                        LocalDate editedOn) {

        Specification<BankMaster> spec = BankSpecification.getFilter(
                id, bankCode, bankName, ifscCode, imps, neft, rtgs,
                activeStatus, createdBy, editedBy, isBlocked, createdOn, editedOn
        );

        return bankMasterRepository.findAll(spec, pageable);
    }

    @Override
    public BankMaster updateBank(Long id,BankRequestDto bankRequestDto){
        log.info("Updating BankMaster with ID: {}", id);
        BankMaster existingBnakMaster = bankMasterRepository.findById(id).orElseThrow(() -> new RuntimeException("Product not found with ID: " + id));

        existingBnakMaster.setBankCode(bankRequestDto.getBankName());
        existingBnakMaster.setBankCode(bankRequestDto.getBankCode());
        existingBnakMaster.setIfscCode(bankRequestDto.getIfscCode());
        existingBnakMaster.setActiveStatus(bankRequestDto.getActiveStatus());
        existingBnakMaster.setImps(bankRequestDto.getImps());
        existingBnakMaster.setRtgs(bankRequestDto.getRtgs());
        existingBnakMaster.setIsBlocked(bankRequestDto.getIsBlocked());
        existingBnakMaster.setEditedBy(bankRequestDto.getEditedBy());
        existingBnakMaster.setEditedOn(LocalDateTime.now());

        BankMaster updateBankMaster = bankMasterRepository.save(existingBnakMaster);
        log.info("BankMaster updated successfully: {}", updateBankMaster);
        return updateBankMaster;

    }

    @Override
    public BankMaster getBankById(Long id) {
        log.info("Fetching bank with ID: {}", id);
        return bankMasterRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Bank not found with ID: " + id));
    }
}
