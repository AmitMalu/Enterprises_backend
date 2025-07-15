package com.im.admin.service.impl;

import com.im.admin.dto.StateDTO;
import com.im.admin.dto.StateRequestDTO;
import com.im.admin.dto.StateResponseDTO;
import com.im.admin.entities.Country;
import com.im.admin.entities.State;
import com.im.admin.repositories.CountryRepository;
import com.im.admin.repositories.StateRepository;
import com.im.admin.response.CommonResponse;
import com.im.admin.service.StateService;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class StateServiceImpl implements StateService {


    @Autowired
    private StateRepository stateRepository;

    @Autowired
    private CountryRepository countryRepository;


    @Override
    public CommonResponse<StateResponseDTO> save(StateRequestDTO dto) {
        State state = new State();
        state.setZone(dto.getZone());
        state.setStateName(dto.getStateName());
        state.setStateCode(dto.getStateCode());
        state.setRegionType(dto.getRegionType());
        state.setBlocked(dto.getBlocked());
        state.setCreatedBy(dto.getCreatedBy());

        Country country = countryRepository.findById(dto.getCountryId())
                .orElseThrow(() -> new RuntimeException("Country not found"));
        state.setCountry(country);

        stateRepository.save(state);
        return CommonResponse.success(toDTO(state), "State saved successfully");
    }

    private StateResponseDTO toDTO(State state) {
        StateResponseDTO dto = new StateResponseDTO();
        dto.setId(state.getId());
        dto.setZone(state.getZone());
        dto.setStateName(state.getStateName());
        dto.setStateCode(state.getStateCode());
        dto.setRegionType(state.getRegionType());
        dto.setCountryName(state.getCountry().getCountryName());
        dto.setCountryId(state.getCountry().getId());
        dto.setBlocked(state.getBlocked());
        dto.setCreatedBy(state.getCreatedBy());
        dto.setEditedBy(state.getEditedBy());
        dto.setCreatedOn(state.getCreatedOn());
        dto.setEditedOn(state.getEditedOn());
        return dto;
    }

    @Override
    public CommonResponse<StateResponseDTO> update(Long id, StateRequestDTO dto) {
        State state = stateRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("State not found"));

        state.setZone(dto.getZone());
        state.setStateName(dto.getStateName());
        state.setStateCode(dto.getStateCode());
        state.setRegionType(dto.getRegionType());
        state.setBlocked(dto.getBlocked());
        state.setEditedBy(dto.getEditedBy());
        state.setEditedOn(LocalDateTime.now());

        Country country = countryRepository.findById(dto.getCountryId())
                .orElseThrow(() -> new RuntimeException("Country not found"));
        state.setCountry(country);

        stateRepository.save(state);
        return CommonResponse.success(toDTO(state), "State updated successfully");
    }


    @Override
    public CommonResponse<StateResponseDTO> getById(Long id) {
        State state = stateRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("State not found"));
        return CommonResponse.success(toDTO(state), "State fetched successfully");
    }


    @Override
    public Page<StateResponseDTO> getAllStates(int page, int size, String zone, String stateName, String stateCode, String regionType, Long countryId, Boolean blocked) {
        List<State> allStates = stateRepository.findAll();

        List<State> filtered = allStates.stream()
                .filter(s -> zone == null || (s.getZone() != null && s.getZone().toLowerCase().contains(zone.toLowerCase())))
                .filter(s -> stateName == null || (s.getStateName() != null && s.getStateName().toLowerCase().contains(stateName.toLowerCase())))
                .filter(s -> stateCode == null || (s.getStateCode() != null && s.getStateCode().toLowerCase().contains(stateCode.toLowerCase())))
                .filter(s -> regionType == null || (s.getRegionType() != null && s.getRegionType().toLowerCase().contains(regionType.toLowerCase())))
                .filter(s -> countryId == null || (s.getCountry() != null && s.getCountry().getId().equals(countryId)))
                .filter(s -> blocked == null || (s.getBlocked() != null && s.getBlocked().equals(blocked)))
                .toList();

        int start = page * size;
        int end = Math.min(start + size, filtered.size());
        List<State> pagedList = (start < end) ? filtered.subList(start, end) : List.of();

        List<StateResponseDTO> dtoList = pagedList.stream().map(this::toDTO).toList();

        return new PageImpl<>(dtoList, PageRequest.of(page, size), filtered.size());
    }


    @Override
    public List<StateDTO> getStatesByCountry(Long countryId) {
        List<State> stateList = stateRepository.findByCountryId(countryId);
        return stateList.stream()
                .map(state -> new StateDTO(state.getId(), state.getStateName()))
                .collect(Collectors.toList());
    }

    @Override
    public ByteArrayResource generateSampleTemplate() throws IOException {
        List<String> headers = List.of("Zone", "State", "State Code", "Region Type", "Country");
        List<List<String>> data = List.of(List.of("Western", "Maharashtra", "MH", "state", "India"),
                                            List.of("Northern","Delhi","DL","union territory","India"));

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("States");

        // Header row
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.size(); i++) {
            headerRow.createCell(i).setCellValue(headers.get(i));
        }

        // Sample data row
        for (int rowIndex = 0; rowIndex < data.size(); rowIndex++) {
            Row row = sheet.createRow(rowIndex + 1);
            List<String> rowData = data.get(rowIndex);
            for (int colIndex = 0; colIndex < rowData.size(); colIndex++) {
                row.createCell(colIndex).setCellValue(rowData.get(colIndex));
            }
        }

        // Convert to ByteArrayResource
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        workbook.close();
        return new ByteArrayResource(out.toByteArray());
    }

    @Override
    public ByteArrayResource saveStatesFromExcel(MultipartFile file, String createdBy) {
        List<String> expectedHeaders = List.of("Zone", "State", "State Code", "Region Type", "Country");

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(0);

            // Validate header
            for (int i = 0; i < expectedHeaders.size(); i++) {
                String actual = getStringCellValue(headerRow.getCell(i));
                String expected = expectedHeaders.get(i);
                if (!expected.equalsIgnoreCase(actual)) {
                    throw new RuntimeException("Invalid header at column " + (i + 1)
                            + ". Expected '" + expected + "' but found '" + actual + "'");
                }
            }

            // Add new columns
            int statusCol = expectedHeaders.size();
            headerRow.createCell(statusCol).setCellValue("Status");
            headerRow.createCell(statusCol + 1).setCellValue("Remark");

            // Process data rows
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                String zone = getStringCellValue(row.getCell(0));
                String stateName = getStringCellValue(row.getCell(1));
                String stateCode = getStringCellValue(row.getCell(2));
                String regionType = getStringCellValue(row.getCell(3));
                String countryName = getStringCellValue(row.getCell(4));

                String status = "Success";
                String remark = "Saved successfully";

                try {
                    if (isBlank(stateName) || isBlank(stateCode) || isBlank(regionType) || isBlank(countryName)) {
                        throw new IllegalArgumentException("Required field(s) missing.");
                    }

                    Country country = countryRepository.findByCountryNameIgnoreCase(countryName.trim())
                            .orElseThrow(() -> new IllegalArgumentException("Country not found - " + countryName));

                    boolean exists = stateRepository.existsByStateNameIgnoreCaseAndStateCodeIgnoreCase(stateName, stateCode);
                    if (exists) {
                        throw new IllegalArgumentException("State already exists.");
                    }

                    State state = new State();
                    state.setZone(zone);
                    state.setStateName(stateName);
                    state.setStateCode(stateCode);
                    state.setRegionType(regionType);
                    state.setCountry(country);
                    state.setBlocked(false);
                    state.setCreatedBy(createdBy);
                    state.setCreatedOn(LocalDateTime.now());

                    stateRepository.save(state);

                } catch (Exception ex) {
                    status = "Fail";
                    remark = ex.getMessage();
                }

                // Set Status & Remark
                row.createCell(statusCol).setCellValue(status);
                row.createCell(statusCol + 1).setCellValue(remark);
            }

            // Return updated file
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return new ByteArrayResource(out.toByteArray());

        } catch (IOException e) {
            throw new RuntimeException("Failed to process Excel file: " + e.getMessage(), e);
        }
    }

    private String getStringCellValue(Cell cell) {
        if (cell == null) return null;
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> String.valueOf((int) cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> null;
        };
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }



}
