package com.im.admin.service.impl;


import com.im.admin.dto.WalletLoadRequestDto;
import com.im.admin.entities.WalletLoadRequest;
import com.im.admin.repositories.WalletLoadRequestRepository;
import com.im.admin.service.WalletLoadRequestService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
@Validated
@Slf4j
public class WalletLoadRequestServiceImpl implements WalletLoadRequestService {

    @Autowired
    private WalletLoadRequestRepository repository;

    private final Path imageStoragePath = Paths.get("uploads/wallet-images");

    @Override
    public String saveWalletLoadRequest(WalletLoadRequestDto dto, MultipartFile imageFile) throws IOException {
        if (!Files.exists(imageStoragePath)) {
            Files.createDirectories(imageStoragePath);
        }

        String fileName = null;
        if (imageFile != null && !imageFile.isEmpty()) {
            fileName = UUID.randomUUID() + "_" + imageFile.getOriginalFilename();
            Path filePath = imageStoragePath.resolve(fileName);
            Files.copy(imageFile.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        }

        WalletLoadRequest entity = new WalletLoadRequest();
        entity.setHierarchyId(dto.getHierarchyId());
        entity.setParentHierarchyId(dto.getParentHierarchyId());
        entity.setMobileNumber(dto.getMobileNumber());
        entity.setDepositAmount(Double.valueOf(dto.getDepositAmount()));
        entity.setDepositImage(fileName);
        entity.setBankName(dto.getBankAccountName());
        entity.setBankAccountNumber(dto.getBankAccountNumber());
        entity.setBankTranId(dto.getBankTranId());
        entity.setPaymentMode(dto.getPaymentMode());
        entity.setStatus("PENDING");

        repository.save(entity);
        return "Wallet load request submitted successfully.";
    }
}
