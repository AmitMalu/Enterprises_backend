package com.im.admin.service.impl;

import com.im.admin.dto.DistrictDTO;
import com.im.admin.dto.DistrictRequestDTO;
import com.im.admin.dto.DistrictResponseDTO;
import com.im.admin.entities.Country;
import com.im.admin.entities.District;
import com.im.admin.entities.State;
import com.im.admin.repositories.CountryRepository;
import com.im.admin.repositories.DistrictRepository;
import com.im.admin.repositories.StateRepository;
import com.im.admin.response.CommonResponse;
import com.im.admin.service.DistrictService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DistrictServiceImpl implements DistrictService {

    @Autowired
    private DistrictRepository districtRepository;

    @Autowired
    private CountryRepository countryRepository;

    @Autowired
    private StateRepository stateRepository;

    @Override
    public CommonResponse<DistrictResponseDTO> save(DistrictRequestDTO dto) {
        log.info("Saving district with request: {}", dto);

        District district = new District();
        district.setDistrictName(dto.getDistrictName());
        district.setDistrictCode(dto.getDistrictCode());
        district.setBlocked(dto.getBlocked());
        district.setCreatedBy(dto.getCreatedBy());

        log.debug("Fetching country with ID: {}", dto.getCountryId());
        district.setCountry(countryRepository.findById(dto.getCountryId()).orElseThrow(() -> new RuntimeException("Country not found")));

        log.debug("Fetching state with ID: {}", dto.getStateId());
        district.setState(stateRepository.findById(dto.getStateId()).orElseThrow(() -> new RuntimeException("State not found")));

        districtRepository.save(district);
        log.info("District saved successfully with ID: {}", district.getId());

        return CommonResponse.success(toDTO(district), "District saved successfully");
    }


    @Override
    public CommonResponse<DistrictResponseDTO> update(Long id, DistrictRequestDTO dto) {
        log.info("Updating district with id: {}", id);

        District district = districtRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("District not found with id: {}", id);
                    return new RuntimeException("District not found");
                });

        log.info("Before Update District: {}", district);

        district.setDistrictName(dto.getDistrictName());
        district.setDistrictCode(dto.getDistrictCode());
        district.setBlocked(dto.getBlocked());
        district.setEditedOn(LocalDateTime.now());
        district.setEditedBy(dto.getEditedBy());

        district.setCountry(countryRepository.findById(dto.getCountryId()).orElseThrow(() -> {
            log.error("Country not found with id: {}", dto.getCountryId());
            return new RuntimeException("Country not found");
        }));
        district.setState(stateRepository.findById(dto.getStateId()).orElseThrow(() -> {
            log.error("State not found with id: {}", dto.getStateId());
            return new RuntimeException("State not found");
        }));

        districtRepository.save(district);
        log.info("After Update District: {}", district);

        return CommonResponse.success(toDTO(district), "District updated successfully");
    }


    @Override
    public CommonResponse<DistrictResponseDTO> getById(Long id) {
        log.info("Fetching district by id: {}", id);

        District district = districtRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("District not found with id: {}", id);
                    return new RuntimeException("District not found");
                });

        log.info("District fetched successfully with id: {}", id);
        return CommonResponse.success(toDTO(district), "District fetched");
    }

    @Override
    public CommonResponse<Page<DistrictResponseDTO>> getAll(int page, int size,
                                                            String districtName, String districtCode, Boolean blocked,
                                                            String createdBy, String editedBy) {
        log.info("Fetching districts with filters - page: {}, size: {}, districtName: {}, districtCode: {}, blocked: {}, createdBy: {}, editedBy: {}",
                page, size, districtName, districtCode, blocked, createdBy, editedBy);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdOn"));

        Specification<District> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (districtName != null && !districtName.isEmpty()) {
                log.debug("Applying filter - districtName contains: {}", districtName);
                predicates.add(cb.like(cb.lower(root.get("districtName")), "%" + districtName.toLowerCase() + "%"));
            }
            if (districtCode != null && !districtCode.isEmpty()) {
                log.debug("Applying filter - districtCode contains: {}", districtCode);
                predicates.add(cb.like(cb.lower(root.get("districtCode")), "%" + districtCode.toLowerCase() + "%"));
            }
            if (blocked != null) {
                log.debug("Applying filter - blocked: {}", blocked);
                predicates.add(cb.equal(root.get("blocked"), blocked));
            }
            if (createdBy != null && !createdBy.isEmpty()) {
                log.debug("Applying filter - createdBy contains: {}", createdBy);
                predicates.add(cb.like(cb.lower(root.get("createdBy")), "%" + createdBy.toLowerCase() + "%"));
            }
            if (editedBy != null && !editedBy.isEmpty()) {
                log.debug("Applying filter - editedBy contains: {}", editedBy);
                predicates.add(cb.like(cb.lower(root.get("editedBy")), "%" + editedBy.toLowerCase() + "%"));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<District> districtPage = districtRepository.findAll(spec, pageable);
        log.info("Total districts fetched: {}", districtPage.getTotalElements());

        Page<DistrictResponseDTO> dtoPage = districtPage.map(this::toDTO);
        log.debug("Mapped district page to DTOs: {}", dtoPage.getContent());

        return CommonResponse.success(dtoPage, "Districts fetched successfully");
    }

    private DistrictResponseDTO toDTO(District district) {
        log.trace("Mapping District entity to DTO: {}", district);

        DistrictResponseDTO dto = new DistrictResponseDTO();
        dto.setId(district.getId());
        dto.setDistrictName(district.getDistrictName());
        dto.setDistrictCode(district.getDistrictCode());
        dto.setBlocked(district.getBlocked());
        dto.setCountryName(district.getCountry().getCountryName());
        dto.setStateName(district.getState().getStateName());
        dto.setStateId(district.getState().getId());
        dto.setCountryId(district.getCountry().getId());
        dto.setCreatedBy(district.getCreatedBy());
        dto.setEditedBy(district.getEditedBy());
        dto.setCreatedOn(district.getCreatedOn());
        dto.setEditedOn(district.getEditedOn());

        log.trace("Mapped DTO: {}", dto);
        return dto;
    }


    @Override
    public List<DistrictDTO> getDistrictsByState(Long stateId) {
        log.info("Fetching districts for stateId: {}", stateId);
        List<District> districts = districtRepository.findByStateId(stateId);
        log.debug("Districts fetched from DB: {}", districts);
        List<DistrictDTO> districtDTOs = districts.stream()
                .map(d -> new DistrictDTO(d.getId(), d.getDistrictName()))
                .collect(Collectors.toList());
        log.info("Mapped {} districts to DTOs for stateId: {}", districtDTOs.size(), stateId);
        return districtDTOs;
    }


    @Override
    public ByteArrayResource generateSampleTemplate() throws IOException {
        log.info("Generating sample District Excel template");

        List<String> headers = List.of(
                "District Name",
                "District Code",
                "Country Name",
                "State Name"
        );

        List<String> sampleRow = List.of(
                "Pune",
                "PN001",
                "India",
                "Maharashtra"
        );

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("District Template");

            // Header Row
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.size(); i++) {
                headerRow.createCell(i).setCellValue(headers.get(i));
            }
            log.debug("Header row created with columns: {}", headers);

            // Sample Data Row
            Row dataRow = sheet.createRow(1);
            for (int i = 0; i < sampleRow.size(); i++) {
                dataRow.createCell(i).setCellValue(sampleRow.get(i));
            }
            log.debug("Sample data row added: {}", sampleRow);

            // Auto-size columns
            for (int i = 0; i < headers.size(); i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            log.info("Sample District template generated successfully");
            return new ByteArrayResource(out.toByteArray());
        }
    }

    @Override
    public ByteArrayInputStream uploadDistrictsFromExcel(MultipartFile file, String uploadedBy) {
        if (file.isEmpty() || !file.getOriginalFilename().endsWith(".xlsx")) {
            throw new IllegalArgumentException("Invalid file. Only .xlsx format supported.");
        }

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);

            // Add Status column at the end
            Row header = sheet.getRow(0);
            if (header == null) header = sheet.createRow(0);
            int statusCol = header.getLastCellNum();
            header.createCell(statusCol).setCellValue("Status");

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                try {
                    String districtName = getCellString(row, 0);
                    String districtCode = getCellString(row, 1);
                    String countryName = getCellString(row, 2);
                    String stateName = getCellString(row, 3);
                    String blockedStr = getCellString(row, 4);

                    if (districtRepository.existsByDistrictCode(districtCode)) {
                        row.createCell(statusCol).setCellValue("Skipped (Duplicate)");
                        continue;
                    }

                    Country country = countryRepository.findByCountryNameIgnoreCase(countryName)
                            .orElseThrow(() -> new RuntimeException("Country not found: " + countryName));

                    State state = stateRepository.findByStateNameIgnoreCaseAndCountry(stateName, country)
                            .orElseThrow(() -> new RuntimeException("State not found: " + stateName + " in country " + countryName));

                    boolean blocked = Boolean.parseBoolean(blockedStr);

                    District district = new District();
                    district.setDistrictName(districtName);
                    district.setDistrictCode(districtCode);
                    district.setCountry(country);
                    district.setState(state);
                    district.setBlocked(blocked);
                    district.setCreatedBy(uploadedBy);
                    district.setCreatedOn(LocalDateTime.now());

                    districtRepository.save(district);
                    row.createCell(statusCol).setCellValue("Inserted");

                } catch (Exception e) {
                    row.createCell(statusCol).setCellValue("Error: " + e.getMessage());
                }
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());

        } catch (IOException e) {
            throw new RuntimeException("Failed to process Excel file", e);
        }
    }


    private String getCellString(Row row, int index) {
        Cell cell = row.getCell(index, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
        cell.setCellType(CellType.STRING);
        return cell.getStringCellValue().trim();
    }

}

