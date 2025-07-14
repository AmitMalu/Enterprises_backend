package com.im.admin.service.impl;

import com.im.admin.entities.BanksDetailss;
import com.im.admin.repositories.BankDetailsRepository;
import com.im.admin.response.CommonResponse;
import com.im.admin.service.BankDetailsService;
import jakarta.persistence.criteria.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

@Service
public class BankDetailsServiceImpl implements BankDetailsService {

    @Autowired
    private BankDetailsRepository repository;

    @Override
    public CommonResponse<BanksDetailss> save(BanksDetailss request) {
        BanksDetailss banksDetailss = new BanksDetailss();

        banksDetailss.setHierarchyId(request.getHierarchyId());
        banksDetailss.setBankName(request.getBankName());
        banksDetailss.setBankAccountNumber(request.getBankAccountNumber());
        banksDetailss.setBankHolderName(request.getBankHolderName());
        banksDetailss.setIfscCode(request.getIfscCode());
        banksDetailss.setIsVerified(request.getIsVerified());
        banksDetailss.setCreatedBy(request.getCreatedBy());
        BanksDetailss saved = repository.save(banksDetailss);
        return CommonResponse.success(saved, "Bank details saved successfully");
    }

    @Override
    public CommonResponse<BanksDetailss> update(Long id, BanksDetailss request) {
        BanksDetailss existing = repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Bank record not found with ID: " + id));

        existing.setHierarchyId(request.getHierarchyId());
        existing.setBankName(request.getBankName());
        existing.setBankHolderName(request.getBankHolderName());
        existing.setBankAccountNumber(request.getBankAccountNumber());
        existing.setIfscCode(request.getIfscCode());
        existing.setIsVerified(request.getIsVerified());

        BanksDetailss updated = repository.save(existing);
        return CommonResponse.success(updated, "Bank details updated successfully");
    }

    @Override
    public CommonResponse<BanksDetailss> getById(Long id) {
        BanksDetailss data = repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Bank record not found with ID: " + id));
        return CommonResponse.success(data, "Bank details fetched");
    }

    @Override
    public CommonResponse<Page<BanksDetailss>> getAll(String hierarchyId, String bankName, Boolean isVerified, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdOn").descending());

        Specification<BanksDetailss> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (hierarchyId != null && !hierarchyId.isEmpty())
                predicates.add(cb.equal(cb.lower(root.get("hierarchyId")), hierarchyId.toLowerCase()));

            if (bankName != null && !bankName.isEmpty())
                predicates.add(cb.like(cb.lower(root.get("bankName")), "%" + bankName.toLowerCase() + "%"));

            if (isVerified != null)
                predicates.add(cb.equal(root.get("isVerified"), isVerified));

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<BanksDetailss> result = repository.findAll(spec, pageable);
        return CommonResponse.success(result, "Bank list loaded");
    }
}

