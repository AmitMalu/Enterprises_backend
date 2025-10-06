package com.im.admin.service.impl;

import com.im.admin.dto.BankDetailsResponseDto;
import com.im.admin.entities.BanksDetails;
import com.im.admin.repositories.BankDetailsRepository;
import com.im.admin.response.CommonResponse;
import com.im.admin.service.BankDetailsService;
import jakarta.persistence.criteria.Predicate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

@Slf4j
@Service
public class BankDetailsServiceImpl implements BankDetailsService {


    @Autowired
    private BankDetailsRepository repository;

    @Override
    public CommonResponse<BanksDetails> save(BanksDetails request) {
        log.info("Saving new bank details | HierarchyId={}, BankName={}, AccountNumber={}",
                request.getHierarchyId(), request.getBankName(), request.getBankAccountNumber());

        // Step 1: Check existing bank count for hierarchyId
        long bankCount = repository.countByHierarchyId(request.getHierarchyId());

        if (bankCount >= 2) {
            log.warn("Max 2 banks already exist for HierarchyId={}", request.getHierarchyId());
            return CommonResponse.error(400, "Only 2 banks are allowed per hierarchy.");
        }

        BanksDetails banksDetailss = new BanksDetails();

        banksDetailss.setHierarchyId(request.getHierarchyId());
        banksDetailss.setBankName(request.getBankName());
        banksDetailss.setBankAccountNumber(request.getBankAccountNumber());
        banksDetailss.setBankHolderName(request.getBankHolderName());
        banksDetailss.setIfscCode(request.getIfscCode());
        banksDetailss.setIsVerified(request.getIsVerified());
        banksDetailss.setCreatedBy(request.getCreatedBy());

        BanksDetails saved = repository.save(banksDetailss);
        log.info("Bank details saved successfully | ID={}", saved.getId());

        return CommonResponse.success(saved, "Bank details saved successfully");
    }

    @Override
    public CommonResponse<BanksDetails> update(Long id, BanksDetails request) {
        log.info("Updating bank details | ID={}, HierarchyId={}, BankName={}",
                id, request.getHierarchyId(), request.getBankName());

        BanksDetails existing = repository.findById(id)
                .orElseThrow(() -> {
                    log.error("Bank record not found with ID={}", id);
                    return new NoSuchElementException("Bank record not found with ID: " + id);
                });

        existing.setHierarchyId(request.getHierarchyId());
        existing.setBankName(request.getBankName());
        existing.setBankHolderName(request.getBankHolderName());
        existing.setBankAccountNumber(request.getBankAccountNumber());
        existing.setIfscCode(request.getIfscCode());
        existing.setIsVerified(request.getIsVerified());

        BanksDetails updated = repository.save(existing);
        log.info("Bank details updated successfully | ID={}", updated.getId());

        return CommonResponse.success(updated, "Bank details updated successfully");
    }


    @Override
    public CommonResponse<BanksDetails> getById(Long id) {
        log.info("Fetching bank details by ID={}", id);

        BanksDetails data = repository.findById(id)
                .orElseThrow(() -> {
                    log.error("Bank record not found with ID={}", id);
                    return new NoSuchElementException("Bank record not found with ID: " + id);
                });

        log.info("Bank details fetched successfully | ID={}", data.getId());
        return CommonResponse.success(data, "Bank details fetched");
    }

    @Override
    public CommonResponse<Page<BanksDetails>> getAll(String hierarchyId, String bankName, Boolean isVerified, int page, int size) {
        log.info("Fetching all banks | hierarchyId={}, bankName={}, isVerified={}, page={}, size={}",
                hierarchyId, bankName, isVerified, page, size);

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdOn").descending());

        Specification<BanksDetails> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (hierarchyId != null && !hierarchyId.isEmpty())
                predicates.add(cb.equal(cb.lower(root.get("hierarchyId")), hierarchyId.toLowerCase()));

            if (bankName != null && !bankName.isEmpty())
                predicates.add(cb.like(cb.lower(root.get("bankName")), "%" + bankName.toLowerCase() + "%"));

            if (isVerified != null)
                predicates.add(cb.equal(root.get("isVerified"), isVerified));

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<BanksDetails> result = repository.findAll(spec, pageable);
        log.info("Bank list loaded successfully | totalRecords={}", result.getTotalElements());

        return CommonResponse.success(result, "Bank list loaded");
    }

    @Override
    public CommonResponse<String> delete(Long id) {
        BanksDetails bank = repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Bank record not found with ID: " + id));

        // Check if 90 days have passed since creation
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime createdOn = bank.getCreatedOn();

        if (createdOn.plusDays(90).isAfter(now)) {
            log.warn("Bank record ID={} cannot be deleted before 90 days", id);
            return CommonResponse.error(400, "Bank record can only be deleted after 90 days of creation.");
        }

        repository.delete(bank);
        log.info("Bank record deleted successfully | ID={}", id);
        return CommonResponse.success("200","Bank record deleted successfully.");
    }

    @Override
    public CommonResponse<List<BankDetailsResponseDto>> getVerifiedBankDetails(String hierarchyId) {
        List<BanksDetails> verifiedBanks = repository.findByHierarchyIdAndIsVerifiedTrue(hierarchyId);

        List<BankDetailsResponseDto> responseDtos = verifiedBanks.stream()
                .map(bank -> new BankDetailsResponseDto(
                        bank.getId(),
                        bank.getBankName(),
                        bank.getBankHolderName(),
                        bank.getBankAccountNumber(),
                        bank.getIfscCode()
                ))
                .toList();

        return new CommonResponse<>(200, "Verified Bank Details Fetched Successfully", responseDtos);
    }

}

