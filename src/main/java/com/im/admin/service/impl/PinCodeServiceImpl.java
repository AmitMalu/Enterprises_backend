package com.im.admin.service.impl;

import com.im.admin.dto.PinCodeDTO;
import com.im.admin.dto.PinCodeRequestDTO;
import com.im.admin.dto.PinCodeResponseDTO;
import com.im.admin.entities.*;
import com.im.admin.repositories.*;
import com.im.admin.response.CommonResponse;
import com.im.admin.response.PinCodeLocationResponse;
import com.im.admin.service.PinCodeService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;


@Service
@RequiredArgsConstructor
public class PinCodeServiceImpl implements PinCodeService {

    @Autowired
    private PinCodeRepository pinCodeRepository;

    @Autowired
    private CountryRepository countryRepository;

    @Autowired
    private StateRepository stateRepository;

    @Autowired
    private DistrictRepository districtRepository;

    @Autowired
    private CityRepository cityRepository;

    @Override
    public CommonResponse<PinCodeResponseDTO> save(PinCodeRequestDTO dto) {

        if (pinCodeRepository.existsByPinCode(dto.getPinCode())) {
            throw new RuntimeException("Pin code already exists");
        }

        PinCode pin = new PinCode();
        pin.setPinCode(dto.getPinCode());
        pin.setBlocked(dto.getBlocked());
        pin.setCreatedBy(dto.getCreatedBy());

        pin.setCountry(countryRepository.findById(dto.getCountryId()).orElseThrow(() -> new RuntimeException("Country not found")));
        pin.setState(stateRepository.findById(dto.getStateId()).orElseThrow(() -> new RuntimeException("State not found")));
        pin.setDistrict(districtRepository.findById(dto.getDistrictId()).orElseThrow(() -> new RuntimeException("District not found")));
        pin.setCity(cityRepository.findById(dto.getCityId()).orElseThrow(() -> new RuntimeException("City not found")));

        pinCodeRepository.save(pin);
        return CommonResponse.success(toDTO(pin), "Pin code saved successfully");
    }

    @Override
    public CommonResponse<PinCodeResponseDTO> update(Long id, PinCodeRequestDTO dto) {
        PinCode pin = pinCodeRepository.findById(id).orElseThrow(() -> new RuntimeException("Pin code not found"));

        if (pinCodeRepository.existsByPinCodeAndIdNot(dto.getPinCode(), id)) {
            throw new RuntimeException("Pin code already exists");
        }

        pin.setPinCode(dto.getPinCode());
        pin.setBlocked(dto.getBlocked());
        pin.setEditedBy(dto.getCreatedBy());
        pin.setEditedOn(LocalDateTime.now());

        pin.setCountry(countryRepository.findById(dto.getCountryId()).orElseThrow(() -> new RuntimeException("Country not found")));
        pin.setState(stateRepository.findById(dto.getStateId()).orElseThrow(() -> new RuntimeException("State not found")));
        pin.setDistrict(districtRepository.findById(dto.getDistrictId()).orElseThrow(() -> new RuntimeException("District not found")));
        pin.setCity(cityRepository.findById(dto.getCityId()).orElseThrow(() -> new RuntimeException("City not found")));

        pinCodeRepository.save(pin);
        return CommonResponse.success(toDTO(pin), "Pin code updated successfully");
    }

    @Override
    public CommonResponse<PinCodeResponseDTO> getById(Long id) {
        PinCode pin = pinCodeRepository.findById(id).orElseThrow(() -> new RuntimeException("Pin code not found"));
        return CommonResponse.success(toDTO(pin), "Pin code fetched successfully");
    }

    @Override
    public CommonResponse<Page<PinCodeResponseDTO>> getAll(int page, int size,
                                                           String pinCode, Boolean blocked, String createdBy, String editedBy) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdOn"));

        Specification<PinCode> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (pinCode != null && !pinCode.isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("pinCode")), "%" + pinCode.toLowerCase() + "%"));
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

        Page<PinCode> pageResult = pinCodeRepository.findAll(spec, pageable);
        Page<PinCodeResponseDTO> dtoPage = pageResult.map(this::toDTO);
        return CommonResponse.success(dtoPage, "Pin codes fetched successfully");
    }

    @Override
    public List<PinCodeDTO> getPinCodesByCity(Long cityId) {
        List<PinCode> pins = pinCodeRepository.findByCity_Id(cityId);
        return pins.stream()
                .map(p -> new PinCodeDTO(p.getId(), p.getPinCode()))
                .collect(Collectors.toList());
    }

    private PinCodeResponseDTO toDTO(PinCode pin) {
        PinCodeResponseDTO dto = new PinCodeResponseDTO();
        dto.setId(pin.getId());
        dto.setPinCode(pin.getPinCode());
        dto.setBlocked(pin.getBlocked());
        dto.setCountryName(pin.getCountry().getCountryName());
        dto.setCountryId(pin.getCountry().getId());
        dto.setStateName(pin.getState().getStateName());
        dto.setStateId(pin.getState().getId());
        dto.setDistrictName(pin.getDistrict().getDistrictName());
        dto.setDistrictId(pin.getDistrict().getId());
        dto.setCityName(pin.getCity().getCityName());
        dto.setCityId(pin.getCity().getId());
        dto.setCreatedOn(pin.getCreatedOn());
        dto.setEditedOn(pin.getEditedOn());
        dto.setCreatedBy(pin.getCreatedBy());
        dto.setEditedBy(pin.getEditedBy());
        return dto;
    }

    @Override
    public PinCodeLocationResponse lookupByPinCode(String pinCode) {
        PinCode pin = pinCodeRepository.findByPinCode(pinCode)
                .orElseThrow(() -> new RuntimeException("Pin code not found"));

        PinCodeLocationResponse response = new PinCodeLocationResponse();
        response.setPinCode(pin.getPinCode());
        response.setCountryName(pin.getCountry().getCountryName());
        response.setCountryId(pin.getCountry().getId());
        response.setStateName(pin.getState().getStateName());
        response.setStateId(pin.getState().getId());
        response.setDistrictName(pin.getDistrict().getDistrictName());
        response.setDistrictId(pin.getDistrict().getId());
        response.setCityName(pin.getCity().getCityName());
        response.setCityId(pin.getCity().getId());

        return response;
    }


    @Override
    public ByteArrayResource generateSampleTemplate() {
        List<String> headers = List.of(
                "Pin Code",
                "Country Name",
                "State Name",
                "District Name",
                "City Name"
        );

        List<String> sampleRow = List.of(
                "400001",
                "India",
                "Maharashtra",
                "Mumbai",
                "Mumbai City"
        );

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Pincode Template");

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.size(); i++) {
                headerRow.createCell(i).setCellValue(headers.get(i));
            }

            Row dataRow = sheet.createRow(1);
            for (int i = 0; i < sampleRow.size(); i++) {
                dataRow.createCell(i).setCellValue(sampleRow.get(i));
            }

            for (int i = 0; i < headers.size(); i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return new ByteArrayResource(out.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate template", e);
        }

    }

    @Override
    public ByteArrayInputStream uploadPinCodesFromExcel(MultipartFile file, String uploadedBy) {
        if (file.isEmpty() || !file.getOriginalFilename().endsWith(".xlsx")) {
            throw new IllegalArgumentException("Invalid file. Only .xlsx format supported.");
        }

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);

            // Add "Status" column header
            Row header = sheet.getRow(0);
            if (header == null) header = sheet.createRow(0);
            int statusCol = header.getLastCellNum(); // new column at the end
            header.createCell(statusCol).setCellValue("Status");

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                try {
                    String pinCode = getCellString(row, 0);
                    String countryName = getCellString(row, 1);
                    String stateName = getCellString(row, 2);
                    String districtName = getCellString(row, 3);
                    String cityName = getCellString(row, 4);

                    if (pinCodeRepository.existsByPinCode(pinCode)) {
                        row.createCell(statusCol).setCellValue("Skipped (Duplicate)");
                        continue;
                    }

                    Country country = countryRepository.findByCountryNameIgnoreCase(countryName)
                            .orElseThrow(() -> new RuntimeException("Country not found: " + countryName));

                    State state = stateRepository.findByStateNameIgnoreCaseAndCountry(stateName, country)
                            .orElseThrow(() -> new RuntimeException("State not found: " + stateName));

                    District district = districtRepository.findByDistrictNameIgnoreCaseAndState(districtName, state)
                            .orElseThrow(() -> new RuntimeException("District not found: " + districtName));

                    City city = cityRepository.findByCityNameIgnoreCaseAndDistrict(cityName, district)
                            .orElseThrow(() -> new RuntimeException("City not found: " + cityName));

                    PinCode pin = new PinCode();
                    pin.setPinCode(pinCode);
                    pin.setCountry(country);
                    pin.setState(state);
                    pin.setDistrict(district);
                    pin.setCity(city);
                    pin.setCreatedBy(uploadedBy);
                    pin.setCreatedOn(LocalDateTime.now());

                    pinCodeRepository.save(pin);
                    row.createCell(statusCol).setCellValue("Inserted");

                } catch (Exception e) {
                    row.createCell(statusCol).setCellValue("Error: " + e.getMessage());
                }
            }

            // Write back updated Excel to stream
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

