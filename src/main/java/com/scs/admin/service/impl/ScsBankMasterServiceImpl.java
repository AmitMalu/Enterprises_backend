package com.scs.admin.service.impl;

import com.scs.admin.entities.ScsBankMaster;
import com.scs.admin.entities.WhiteLabels;
import com.scs.admin.repositories.ScsBankMasterRepository;
import com.scs.admin.request.ScsBankMasterRequestDTO;
import com.scs.admin.response.BankResponseDto;
import com.scs.admin.response.CommonResponse;
import com.scs.admin.response.ScsBankDropdownResponse;
import com.scs.admin.response.ScsBankMasterResponseDTO;
import com.scs.admin.service.ScsBankMasterService;
import com.scs.admin.util.CommonMethods;
import com.scs.admin.util.TokenUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Predicate;


@Service
@Slf4j
public class ScsBankMasterServiceImpl implements ScsBankMasterService {

    @Autowired
    private ScsBankMasterRepository repository;

    @Autowired
    private CommonMethods commonMethods;

    @Autowired
    private TokenUtil tokenUtil;

    @Override
    public CommonResponse<ScsBankMasterResponseDTO> create(ScsBankMasterRequestDTO dto) {
        ScsBankMaster entity = mapToEntity(dto);
        entity.setCreatedBy(dto.getCreatedBy());
        ScsBankMaster saved = repository.save(entity);
        return CommonResponse.success(mapToDto(saved), "Bank created successfully");
    }

    @Override
    public CommonResponse<ScsBankMasterResponseDTO> update(Long id, ScsBankMasterRequestDTO dto) {
        ScsBankMaster entity = repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Bank not found"));
        updateEntity(entity, dto);
        ScsBankMaster updated = repository.save(entity);
        return CommonResponse.success(mapToDto(updated), "Bank updated successfully");
    }

    @Override
    public CommonResponse<ScsBankMasterResponseDTO> getById(Long id) {
        ScsBankMaster entity = repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Bank not found"));
        return CommonResponse.success(mapToDto(entity), "Bank details fetched successfully");
    }

    @Override
    public CommonResponse<Page<ScsBankMaster>> getAll(
            Long id,
            String bankName,
            String accountNumber,
            String ifscCode,
            Boolean charges,
            String chargesType,
            Boolean isActive,
            Boolean isBlocked,
            String createdBy,
            String editedOn,
            int page,
            int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdOn").descending());

        Specification<ScsBankMaster> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();


            if (id != null)
                predicates.add(cb.equal(root.get("id"), id));

            if (bankName != null && !bankName.isEmpty())
                predicates.add(cb.like(cb.lower(root.get("bankName")), "%" + bankName.toLowerCase() + "%"));

            if (accountNumber != null && !accountNumber.isEmpty())
                predicates.add(cb.like(cb.lower(root.get("accountNumber")), "%" + accountNumber.toLowerCase() + "%"));

            if (ifscCode != null && !ifscCode.isEmpty())
                predicates.add(cb.like(cb.lower(root.get("ifscCode")), "%" + ifscCode.toLowerCase() + "%"));

            if (charges != null)

                predicates.add(cb.equal(root.get("charges"), charges));

            if (chargesType != null && !chargesType.isEmpty())
                predicates.add(cb.equal(cb.lower(root.get("chargesType")), chargesType.toLowerCase()));

            if (isActive != null)
                predicates.add(cb.equal(root.get("isActive"), isActive));

            if (isBlocked != null)
                predicates.add(cb.equal(root.get("isBlocked"), isBlocked));

            if (createdBy != null && !createdBy.isEmpty())
                predicates.add(cb.equal(cb.lower(root.get("createdBy")), createdBy.toLowerCase()));

            if (editedOn != null && !editedOn.isEmpty())
                predicates.add(cb.like(cb.lower(root.get("editedOn").as(String.class)), "%" + editedOn.toLowerCase() + "%"));


            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<ScsBankMaster> result = repository.findAll(spec, pageable);
        return CommonResponse.success(result, "Bank list fetched successfully");
    }


    // Utility methods
    private ScsBankMaster mapToEntity(ScsBankMasterRequestDTO dto) {
        ScsBankMaster e = new ScsBankMaster();
        updateEntity(e, dto);
        return e;
    }

    private void updateEntity(ScsBankMaster entity, ScsBankMasterRequestDTO dto) {
        entity.setBankName(dto.getBankName());
        entity.setAccountNumber(dto.getAccountNumber());
        entity.setIfscCode(dto.getIfscCode());
        entity.setCharges(dto.getCharges());
        entity.setChargesType(dto.getChargesType());
        entity.setIsActive(dto.getIsActive());
        entity.setIsBlocked(dto.getIsBlocked());
        entity.setIsWhiteLabelAdmin(Boolean.TRUE);
        entity.setWhitelabelId(dto.getWhitelabelId());
        entity.setCreatedBy(entity.getCreatedBy());
        entity.setEditedBy(dto.getEditedBy());
        entity.setEditedOn(LocalDateTime.now());
    }

    private ScsBankMasterResponseDTO mapToDto(ScsBankMaster e) {
        ScsBankMasterResponseDTO d = new ScsBankMasterResponseDTO();
        BeanUtils.copyProperties(e, d);
        return d;
    }

    @Override
    public CommonResponse<List<ScsBankDropdownResponse>> getDropdown() {
        List<ScsBankMaster> banks = repository.findForDropdown();

        List<ScsBankDropdownResponse> responseList = banks.stream()
                .map(bank -> new ScsBankDropdownResponse(bank.getId(), bank.getBankName() + " #" + bank.getAccountNumber()))
                .collect(Collectors.toList());

        return CommonResponse.success(responseList, "Bank dropdown loaded");
    }

    public List<BankResponseDto> getBankDetails() {
        return repository.getActiveBankDetails();
    }

    @Override
    public List<BankResponseDto> getWhiteLabelBankDetails() {

        try {

            String loginHierarchyId = tokenUtil.extractHierarchyId();
            log.info("Fetching White Label bank details for HierarchyId={}", loginHierarchyId);

            String currentHierarchyId = loginHierarchyId;

            while (true) {

                Optional<String> parentOpt = commonMethods.findParentHierarchy(currentHierarchyId);

                if (parentOpt.isEmpty()) {
                    log.warn("No parent hierarchy found for {}. White Label not found.",
                            currentHierarchyId);
                    return Collections.emptyList();
                }

                String parentHierarchyId = parentOpt.get();
                String parentHierarchyType = commonMethods.extractHierarchyType(parentHierarchyId);

                log.info("Parent Hierarchy Found | Id={} | Type={}",
                        parentHierarchyId,
                        parentHierarchyType);

                if ("WL".equalsIgnoreCase(parentHierarchyType)) {

                    log.info("White Label found. Fetching bank details for WL={}",
                            parentHierarchyId);

                    List<ScsBankMaster> bankList =
                            repository.findByWhitelabelIdAndIsActiveTrue(parentHierarchyId);

                    log.info("Found {} active bank(s) for White Label={}",
                            bankList.size(),
                            parentHierarchyId);

                    List<BankResponseDto> response =
                            bankList.stream()
                                    .map(bank -> new BankResponseDto(
                                            bank.getBankName(),
                                            bank.getAccountNumber()
                                    ))
                                    .toList();

                    log.info("Returning {} bank detail(s).", response.size());

                    return response;
                }

                // Continue traversing upward
                currentHierarchyId = parentHierarchyId;
            }

        } catch (Exception e) {

            log.error("Error fetching White Label bank details.", e);
            return Collections.emptyList();
        }
    }
}

