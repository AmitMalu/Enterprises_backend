package com.im.admin.service.impl;


import com.im.admin.dto.PrefundRequestDto;
import com.im.admin.entities.PrefundRequest;
import com.im.admin.repositories.PrefundRequestRepository;
import com.im.admin.response.CommonResponse;
import com.im.admin.service.PrefundRequestService;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Validated
@Slf4j
public class PrefundRequestServiceImpl implements PrefundRequestService {

    @Autowired
    private PrefundRequestRepository repository;

    private final Path imageStoragePath = Paths.get("uploads/wallet-load-images");

    @Override
    public CommonResponse<PrefundRequest> saveWalletLoadRequest(PrefundRequestDto dto, MultipartFile imageFile) throws IOException {

        double depositAmount = Double.parseDouble(dto.getDepositAmount());
        double confirmAmount = Double.parseDouble(dto.getConfirmAmount());

        if (depositAmount != confirmAmount) {
            throw new IllegalArgumentException("Deposit amount and confirmation amount do not match.");
        }

        Optional<PrefundRequest> approved = repository.findApprovedByBankTranId(dto.getBankTranId());
        if (approved.isPresent()) {
            throw new IllegalArgumentException("This transaction has already been approved and cannot be re-submitted.");
        }

        LocalDateTime fiveMinutesAgo = LocalDateTime.now().minusMinutes(5);
        List<PrefundRequest> duplicates = repository.findDuplicatesWithinTime(dto.getBankTranId(), depositAmount, fiveMinutesAgo);
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
        PrefundRequest entity = new PrefundRequest();
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
        if(dto.getDepositDate() != null && !dto.getDepositDate().isEmpty()){
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            entity.setDepositDate(LocalDate.parse(dto.getDepositDate()));
        }

        PrefundRequest saved = repository.save(entity);

        return new CommonResponse<>(200, "Wallet load request submitted successfully", saved);
    }


}
