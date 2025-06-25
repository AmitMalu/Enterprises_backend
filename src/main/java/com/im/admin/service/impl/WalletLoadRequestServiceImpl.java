package com.im.admin.service.impl;


import com.im.admin.dto.WalletLoadRequestDto;
import com.im.admin.entities.WalletLoadRequest;
import com.im.admin.repositories.WalletLoadRequestRepository;
import com.im.admin.response.CommonResponse;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Validated
@Slf4j
public class WalletLoadRequestServiceImpl implements WalletLoadRequestService {

    @Autowired
    private WalletLoadRequestRepository repository;

    private final Path imageStoragePath = Paths.get("uploads/wallet-load-images");

    @Override
    public CommonResponse<WalletLoadRequest> saveWalletLoadRequest(WalletLoadRequestDto dto, MultipartFile imageFile) throws IOException {

        double depositAmount = Double.parseDouble(dto.getDepositAmount());
        double confirmAmount = Double.parseDouble(dto.getConfirmAmount());

        if (depositAmount != confirmAmount) {
            throw new IllegalArgumentException("Deposit amount and confirmation amount do not match.");
        }

        Optional<WalletLoadRequest> approved = repository.findApprovedByBankTranId(dto.getBankTranId());
        if (approved.isPresent()) {
            throw new IllegalArgumentException("This transaction has already been approved and cannot be re-submitted.");
        }

        LocalDateTime fiveMinutesAgo = LocalDateTime.now().minusMinutes(5);
        List<WalletLoadRequest> duplicates = repository.findDuplicatesWithinTime(dto.getBankTranId(), depositAmount, fiveMinutesAgo);
        if (!duplicates.isEmpty()) {
            throw new IllegalArgumentException("Duplicate request detected within last 5 minutes.");
        }

        String fileName = null;
        if (imageFile != null && !imageFile.isEmpty()) {
            if (!Files.exists(imageStoragePath)) {
                Files.createDirectories(imageStoragePath);
            }
            fileName = UUID.randomUUID() + "_" + imageFile.getOriginalFilename();
            Path filePath = imageStoragePath.resolve(fileName);
            Files.copy(imageFile.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        }

        // Save new record
        WalletLoadRequest entity = new WalletLoadRequest();
        entity.setHierarchyId(dto.getHierarchyId());
        entity.setMobileNumber(dto.getMobileNumber());
        entity.setDepositAmount(depositAmount);
        entity.setDepositImage(fileName);
        entity.setBankAccountName(dto.getBankAccountName());
        entity.setBankHolderName(dto.getBankHolderName());
        entity.setBankAccountNumber(dto.getBankAccountNumber());
        entity.setBankTranId(dto.getBankTranId());
        entity.setPaymentMode(dto.getPaymentMode());
        entity.setNarration(dto.getNarration());
        entity.setIpAddress(dto.getIpAddress());

        WalletLoadRequest saved = repository.save(entity);

        return new CommonResponse<>(200, "Wallet load request submitted successfully", saved);
    }


}
