package com.im.admin.service.impl;

import com.im.admin.dto.BankRequestDto;
import com.im.admin.entities.NetworkBankMaster;
import com.im.admin.repositories.NetworkBankMasterRepository;
import com.im.admin.response.BankInfoResponse;
import com.im.admin.response.NetworkBankDropdownResponse;
import com.im.admin.service.NetworkBankService;
import com.im.admin.spec.BankSpecification;
import com.im.admin.util.FileValidatorUtil;
import com.im.admin.util.ImageHandlerUtil;
import com.im.admin.util.ImageUtil;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;


@Slf4j
@Service
public class NetworkBankServiceImpl implements NetworkBankService {

    @Autowired
    private NetworkBankMasterRepository bankMasterRepository;

    @Autowired
    private ImageUtil imageUtil;


    @Override
    public NetworkBankMaster addBank(BankRequestDto dto, MultipartFile logoFile) throws IOException {
        if (dto.getBankCode() == null || dto.getBankName() == null) {
            throw new IllegalArgumentException("Bank Code and Bank Name are required.");
        }

        NetworkBankMaster bank = getNetworkBankMaster(dto);

        try {
            if (logoFile != null && !logoFile.isEmpty()) {
                String imagePath = imageUtil.saveImage(logoFile, "Bank_Logos");
                bank.setLogo(imagePath);
            }
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }

        return bankMasterRepository.save(bank);
    }

    private static NetworkBankMaster getNetworkBankMaster(BankRequestDto dto) {
        NetworkBankMaster bank = new NetworkBankMaster();
        bank.setBankCode(dto.getBankCode());
        bank.setBankCodeBI(dto.getBankCodeBi());
        bank.setBankName(dto.getBankName());
        bank.setBankBranchName(dto.getBankBranchName());
        bank.setIfscCode(dto.getIfscCode());
        bank.setImps(dto.getImps());
        bank.setNeft(dto.getNeft());
        bank.setRtgs(dto.getRtgs());
        bank.setIsActive(dto.getIsActive());
        bank.setCreatedBy(dto.getCreatedBy());
        bank.setEditedBy(dto.getEditedBy());
        bank.setIsBlocked(dto.getIsBlocked());
        bank.setMaxAccountNoLength(dto.getMaxAccountNoLength());
        return bank;
    }


    @Override
    public Page<NetworkBankMaster> getAllBanks(
            Pageable pageable,
            String id,
            String bankCode,
            String bankName,
            String bankBranchName,
            String ifscCode,
            Boolean imps,
            Boolean neft,
            Boolean rtgs,
            Boolean isActive,
            String createdBy,
            String editedBy,
            Boolean isBlocked,
            LocalDate createdOn,
            LocalDate editedOn
    ) {
        Specification<NetworkBankMaster> spec = BankSpecification.getFilter(
                id, bankCode, bankName, bankBranchName, ifscCode,
                imps, neft, rtgs, isActive,
                createdBy, editedBy, isBlocked, createdOn, editedOn
        );

        return bankMasterRepository.findAll(spec, pageable);
    }


    @Override
    public NetworkBankMaster updateBank(Long id, BankRequestDto dto, MultipartFile logoFile) {
        log.info("Updating BankMaster with ID: {}", id);

        NetworkBankMaster existing = bankMasterRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Bank not found with ID: " + id));

        existing.setBankCode(dto.getBankCode());
        existing.setBankCodeBI(dto.getBankCodeBi());
        existing.setBankName(dto.getBankName());
        existing.setBankBranchName(dto.getBankBranchName());
        existing.setIfscCode(dto.getIfscCode());
        existing.setImps(dto.getImps());
        existing.setNeft(dto.getNeft());
        existing.setRtgs(dto.getRtgs());
        existing.setIsActive(dto.getIsActive());
        existing.setIsBlocked(dto.getIsBlocked());
        existing.setEditedBy(dto.getEditedBy());
        existing.setEditedOn(LocalDateTime.now());
        existing.setMaxAccountNoLength(dto.getMaxAccountNoLength());

        // Optional: handle logo re-upload
        if (logoFile != null && !logoFile.isEmpty()) {
            try {
                String imagePath = imageUtil.saveImage(logoFile, "Bank_Logos");
                existing.setLogo(imagePath);
            } catch (IOException e) {
                throw new RuntimeException("Logo upload failed", e);
            }
        }

        return bankMasterRepository.save(existing);
    }


    @Override
    public NetworkBankMaster getBankById(Long id) {
        log.info("Fetching bank with ID: {}", id);
        return bankMasterRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Bank not found with ID: " + id));
    }

    public List<NetworkBankDropdownResponse> getBankDropdownList() {
        return bankMasterRepository.getActiveBankDropdown();
    }

    @Override
    public BankInfoResponse getBankInfoByIfsc(String ifscCode) {
        NetworkBankMaster bank = bankMasterRepository.findByIfscCodeIgnoreCase(ifscCode.trim())
                .orElseThrow(() -> new NoSuchElementException("No bank found with IFSC: " + ifscCode));

        return new BankInfoResponse(bank.getBankName(), bank.getBankBranchName());
    }

}
