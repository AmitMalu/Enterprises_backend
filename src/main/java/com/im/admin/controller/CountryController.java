package com.im.admin.controller;

import com.im.admin.dto.CountryDropdownDto;
import com.im.admin.entities.Country;
import com.im.admin.response.CommonResponse;
import com.im.admin.service.CountryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/country")
public class CountryController {

    @Autowired
    private CountryService countryService;

    @PostMapping
    public ResponseEntity<CommonResponse<Country>> create(@RequestBody Country country) {
        log.info("API Call: Create Country, Request Body: {}", country);
        Country saved = countryService.createCountry(country);
        log.info("Country Created Successfully: {}", saved);
        return ResponseEntity.ok(new CommonResponse<>(201, "Country added successfully", saved));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CommonResponse<Country>> getById(@PathVariable Long id) {
        log.info("API Call: Get Country by ID: {}", id);
        return countryService.getCountryById(id)
                .map(country -> {
                    log.info("Country Fetched Successfully: {}", country);
                    return ResponseEntity.ok(new CommonResponse<>(200, "Country fetched", country));
                })
                .orElseGet(() -> {
                    log.warn("Country Not Found with ID: {}", id);
                    return ResponseEntity.status(404).body(new CommonResponse<>(404, "Country not found", null));
                });
    }

    @GetMapping
    public ResponseEntity<CommonResponse<Page<Country>>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long id,
            @RequestParam(required = false) String country,
            @RequestParam(required = false) String countryCode,
            @RequestParam(required = false) Boolean blocked,
            @RequestParam(required = false) String createdBy,
            @RequestParam(required = false) String editedBy,
            @RequestParam(required = false) String createdOn
    ) {
        log.info("API Call: Get All Countries with filters -> page={}, size={}, id={}, country={}, countryCode={}, blocked={}, createdBy={}, editedBy={}, createdOn={}",
                page, size, id, country, countryCode, blocked, createdBy, editedBy, createdOn);
        Page<Country> filteredCountries = countryService.getFilteredCountries(page, size,
                id, country, countryCode, blocked, createdBy, editedBy, createdOn);
        log.info("Fetched {} countries", filteredCountries.getContent().size());
        return ResponseEntity.ok(new CommonResponse<>(200, "Countries fetched", filteredCountries));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CommonResponse<Country>> update(@PathVariable Long id, @RequestBody Country country) {
        log.info("API Call: Update Country, ID: {}, Request Body: {}", id, country);
        try {
            Country updated = countryService.updateCountry(id, country);
            log.info("Country Updated Successfully: {}", updated);
            return ResponseEntity.ok(new CommonResponse<>(200, "Country updated", updated));
        } catch (RuntimeException e) {
            log.error("Country Update Failed for ID: {}, Error: {}", id, e.getMessage());
            return ResponseEntity.status(404).body(new CommonResponse<>(404, e.getMessage(), null));
        }
    }

    @GetMapping("/dropdown")
    public ResponseEntity<CommonResponse<List<CountryDropdownDto>>> getCountryDropdown() {
        log.info("API Call: Get Country Dropdown");
        List<CountryDropdownDto> dropdownList = countryService.getCountryDropdown();
        log.info("Fetched {} countries for dropdown", dropdownList.size());
        return ResponseEntity.ok(new CommonResponse<>(200, "Country dropdown fetched", dropdownList));
    }

}
