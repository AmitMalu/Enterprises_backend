package com.im.admin.service.impl;

import com.im.admin.dto.CityDTO;
import com.im.admin.dto.CityRequestDTO;
import com.im.admin.dto.CityResponseDTO;
import com.im.admin.entities.City;
import com.im.admin.entities.Country;
import com.im.admin.entities.District;
import com.im.admin.entities.State;
import com.im.admin.repositories.CityRepository;
import com.im.admin.repositories.CountryRepository;
import com.im.admin.repositories.DistrictRepository;
import com.im.admin.repositories.StateRepository;
import com.im.admin.response.CommonResponse;
import com.im.admin.service.CityService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CityServiceImpl implements CityService {

    @Autowired
    private CityRepository cityRepository;

    @Autowired
    private CountryRepository countryRepository;

    @Autowired
    private StateRepository stateRepository;

    @Autowired
    private DistrictRepository districtRepository;

    @Override
    public CommonResponse<CityResponseDTO> save(CityRequestDTO dto) {
        log.info("Saving City | DTO: {}", dto);

        City city = new City();
        city.setCityName(dto.getCityName());
        city.setCityCode(dto.getCityCode());
        city.setBlocked(dto.getBlocked());
        city.setCreatedBy(dto.getCreatedBy());

        city.setCountry(countryRepository.findById(dto.getCountryId())
                .orElseThrow(() -> {
                    log.error("Country not found with ID={}", dto.getCountryId());
                    return new RuntimeException("Country not found");
                }));
        city.setState(stateRepository.findById(dto.getStateId())
                .orElseThrow(() -> {
                    log.error("State not found with ID={}", dto.getStateId());
                    return new RuntimeException("State not found");
                }));
        city.setDistrict(districtRepository.findById(dto.getDistrictId())
                .orElseThrow(() -> {
                    log.error("District not found with ID={}", dto.getDistrictId());
                    return new RuntimeException("District not found");
                }));

        cityRepository.save(city);
        log.info("City saved successfully | ID={}, Name={}", city.getId(), city.getCityName());

        return CommonResponse.success(toDTO(city), "City saved successfully");
    }


    @Override
    public CommonResponse<CityResponseDTO> update(Long id, CityRequestDTO dto) {
        log.info("Updating City | ID={}, DTO={}", id, dto);

        City city = cityRepository.findById(id).orElseThrow(() -> {
            log.error("City not found with ID={}", id);
            return new RuntimeException("City not found");
        });

        city.setCityName(dto.getCityName());
        city.setCityCode(dto.getCityCode());
        city.setBlocked(dto.getBlocked());
        city.setEditedOn(LocalDateTime.now());
        city.setEditedBy(dto.getCreatedBy());

        city.setCountry(countryRepository.findById(dto.getCountryId()).orElseThrow(() -> {
            log.error("Country not found with ID={}", dto.getCountryId());
            return new RuntimeException("Country not found");
        }));
        city.setState(stateRepository.findById(dto.getStateId()).orElseThrow(() -> {
            log.error("State not found with ID={}", dto.getStateId());
            return new RuntimeException("State not found");
        }));
        city.setDistrict(districtRepository.findById(dto.getDistrictId()).orElseThrow(() -> {
            log.error("District not found with ID={}", dto.getDistrictId());
            return new RuntimeException("District not found");
        }));

        cityRepository.save(city);
        log.info("City updated successfully | ID={}, Name={}", city.getId(), city.getCityName());

        return CommonResponse.success(toDTO(city), "City updated successfully");
    }


    @Override
    public CommonResponse<CityResponseDTO> getById(Long id) {
        log.info("Fetching City by ID={}", id);

        City city = cityRepository.findById(id).orElseThrow(() -> {
            log.error("City not found with ID={}", id);
            return new RuntimeException("City not found");
        });

        log.info("City fetched successfully | ID={}, Name={}", city.getId(), city.getCityName());
        return CommonResponse.success(toDTO(city), "City fetched successfully");
    }

    @Override
    public CommonResponse<Page<CityResponseDTO>> getAll(int page, int size,
                                                        String cityName, String cityCode, Boolean blocked, String createdBy, String editedBy) {

        log.info("Fetching Cities | page={}, size={}, cityName={}, cityCode={}, blocked={}, createdBy={}, editedBy={}",
                page, size, cityName, cityCode, blocked, createdBy, editedBy);

        Pageable pageable = PageRequest.of(page, size);

        Specification<City> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (cityName != null && !cityName.isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("cityName")), "%" + cityName.toLowerCase() + "%"));
            }
            if (cityCode != null && !cityCode.isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("cityCode")), "%" + cityCode.toLowerCase() + "%"));
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

        Page<City> pageResult = cityRepository.findAll(spec, pageable);
        log.info("Fetched {} cities", pageResult.getNumberOfElements());

        Page<CityResponseDTO> dtoPage = pageResult.map(this::toDTO);
        return CommonResponse.success(dtoPage, "Cities fetched successfully");
    }

    @Override
    public List<CityDTO> getCitiesByDistrict(Long districtId) {
        log.info("Fetching cities for districtId={}", districtId);

        List<City> cities = cityRepository.findByDistrict_Id(districtId);
        log.info("Found {} cities for districtId={}", cities.size(), districtId);

        return cities.stream()
                .map(c -> new CityDTO(c.getId(), c.getCityName()))
                .collect(Collectors.toList());
    }


    private CityResponseDTO toDTO(City city) {
        log.info("Converting City entity to DTO | CityId={}", city.getId());

        CityResponseDTO dto = new CityResponseDTO();
        dto.setId(city.getId());
        dto.setCityName(city.getCityName());
        dto.setCityCode(city.getCityCode());
        dto.setBlocked(city.getBlocked());
        dto.setCountryName(city.getCountry().getCountryName());
        dto.setStateName(city.getState().getStateName());
        dto.setDistrictName(city.getDistrict().getDistrictName());
        dto.setCountryId(city.getCountry().getId());
        dto.setDistrictId(city.getDistrict().getId());
        dto.setStateId(city.getState().getId());
        dto.setCreatedOn(city.getCreatedOn());
        dto.setEditedOn(city.getEditedOn());
        dto.setCreatedBy(city.getEditedBy());
        dto.setEditedBy(city.getEditedBy());

        log.info("Converted City DTO: {}", dto);
        return dto;
    }

    @Override
    public ByteArrayResource generateCitySampleTemplate() throws IOException {
        log.info("Generating sample city Excel template");

        List<String> headers = List.of(
                "City Name",
                "City Code",
                "Country Name",
                "State Name",
                "District Name"
        );

        List<String> sampleRow = List.of(
                "Nagpur",
                "NGP001",
                "India",
                "Maharashtra",
                "Nagpur"
        );

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("City Template");

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
            log.info("Sample city Excel template generated successfully");
            return new ByteArrayResource(out.toByteArray());
        }
    }


    @Override
    public ByteArrayResource uploadCitiesFromExcel(MultipartFile file, String uploadedBy) {
        if (file.isEmpty() || !file.getOriginalFilename().endsWith(".xlsx")) {
            throw new IllegalArgumentException("Invalid file. Only .xlsx format supported.");
        }

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);

            // Add Status & Remark columns
            Row header = sheet.getRow(0);
            int statusCol = header.getLastCellNum();
            header.createCell(statusCol).setCellValue("Status");
            header.createCell(statusCol + 1).setCellValue("Remark");

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                String status = "Success";
                String remark = "Saved successfully";

                try {
                    String cityName = getCellString(row, 0);
                    String cityCode = getCellString(row, 1);
                    String countryName = getCellString(row, 2);
                    String stateName = getCellString(row, 3);
                    String districtName = getCellString(row, 4);

                    if (cityRepository.existsByCityCode(cityCode)) {
                        status = "Skipped";
                        remark = "Duplicate city code";
                    } else {
                        Country country = countryRepository.findByCountryNameIgnoreCase(countryName)
                                .orElseThrow(() -> new RuntimeException("Country not found: " + countryName));

                        State state = stateRepository.findByStateNameIgnoreCaseAndCountry(stateName, country)
                                .orElseThrow(() -> new RuntimeException("State not found: " + stateName));

                        District district = districtRepository.findByDistrictNameIgnoreCaseAndState(districtName, state)
                                .orElseThrow(() -> new RuntimeException("District not found: " + districtName));

                        City city = new City();
                        city.setCityName(cityName);
                        city.setCityCode(cityCode);
                        city.setCountry(country);
                        city.setState(state);
                        city.setDistrict(district);
                        city.setCreatedBy(uploadedBy);
                        city.setCreatedOn(LocalDateTime.now());

                        cityRepository.save(city);
                    }
                } catch (Exception e) {
                    status = "Fail";
                    remark = e.getMessage();
                }

                // Set Status & Remark
                row.createCell(statusCol).setCellValue(status);
                row.createCell(statusCol + 1).setCellValue(remark);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return new ByteArrayResource(out.toByteArray());

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
