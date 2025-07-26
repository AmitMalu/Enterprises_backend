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
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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
        District district = new District();
        district.setDistrictName(dto.getDistrictName());
        district.setDistrictCode(dto.getDistrictCode());
        district.setBlocked(dto.getBlocked());
        district.setCreatedBy(dto.getCreatedBy());

        district.setCountry(countryRepository.findById(dto.getCountryId()).orElseThrow(() -> new RuntimeException("Country not found")));
        district.setState(stateRepository.findById(dto.getStateId()).orElseThrow(() -> new RuntimeException("State not found")));

        districtRepository.save(district);

        return CommonResponse.success(toDTO(district), "District saved successfully");
    }

    @Override
    public CommonResponse<DistrictResponseDTO> update(Long id, DistrictRequestDTO dto) {
        District district = districtRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("District not found"));

        district.setDistrictName(dto.getDistrictName());
        district.setDistrictCode(dto.getDistrictCode());
        district.setBlocked(dto.getBlocked());
        district.setEditedOn(LocalDateTime.now());
        district.setEditedBy(dto.getEditedBy());

        district.setCountry(countryRepository.findById(dto.getCountryId()).orElseThrow(() -> new RuntimeException("Country not found")));
        district.setState(stateRepository.findById(dto.getStateId()).orElseThrow(() -> new RuntimeException("State not found")));

        districtRepository.save(district);
        return CommonResponse.success(toDTO(district), "District updated successfully");
    }

    @Override
    public CommonResponse<DistrictResponseDTO> getById(Long id) {
        District district = districtRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("District not found"));
        return CommonResponse.success(toDTO(district), "District fetched");
    }

    @Override
    public CommonResponse<Page<DistrictResponseDTO>> getAll(int page, int size,
                                                            String districtName, String districtCode, Boolean blocked, String createdBy, String editedBy) {

        Pageable pageable = PageRequest.of(page, size);

        Specification<District> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (districtName != null && !districtName.isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("districtName")), "%" + districtName.toLowerCase() + "%"));
            }
            if (districtCode != null && !districtCode.isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("districtCode")), "%" + districtCode.toLowerCase() + "%"));
            }
            if (blocked != null) {
                predicates.add(cb.equal(root.get("blocked"), blocked));
            }
            if (createdBy != null && !createdBy.isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("createdBy")), "%" + createdBy.toLowerCase() + "%"));
            }
            if (editedBy != null && !editedBy.isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("editedBy")), "%" + editedBy.toLowerCase() + "%"));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<District> districtPage = districtRepository.findAll(spec, pageable);
        Page<DistrictResponseDTO> dtoPage = districtPage.map(this::toDTO);
        return CommonResponse.success(dtoPage, "Districts fetched successfully");
    }


    private DistrictResponseDTO toDTO(District district) {
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
        return dto;
    }

    @Override
    public List<DistrictDTO> getDistrictsByState(Long stateId) {
        List<District> districts = districtRepository.findByStateId(stateId);
        return districts.stream()
                .map(d -> new DistrictDTO(d.getId(), d.getDistrictName()))
                .collect(Collectors.toList());
    }

    @Override
    public ByteArrayResource generateSampleTemplate() throws IOException {
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

            // Sample Data Row
            Row dataRow = sheet.createRow(1);
            for (int i = 0; i < sampleRow.size(); i++) {
                dataRow.createCell(i).setCellValue(sampleRow.get(i));
            }

            // Auto-size columns
            for (int i = 0; i < headers.size(); i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return new ByteArrayResource(out.toByteArray());
        }
    }

    @Override
    public String uploadDistrictsFromExcel(MultipartFile file, String uploadedBy) throws IOException {
        if (file.isEmpty() || !file.getOriginalFilename().endsWith(".xlsx")) {
            return "Invalid file. Only .xlsx format supported.";
        }

        int success = 0, skipped = 0;
        List<String> errors = new ArrayList<>();

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);

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
                        skipped++;
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
                    success++;

                } catch (Exception e) {
                    skipped++;
                    errors.add("Row " + (i + 1) + ": " + e.getMessage());
                }
            }
        }

        return "Upload complete. Inserted: " + success + ", Skipped: " + skipped +
                (errors.isEmpty() ? "" : ". Errors: " + String.join("; ", errors));
    }

    private String getCellString(Row row, int index) {
        Cell cell = row.getCell(index, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
        cell.setCellType(CellType.STRING);
        return cell.getStringCellValue().trim();
    }

}

