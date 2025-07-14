package com.im.admin.controller;

import com.im.admin.dto.CountryDropdownDto;
import com.im.admin.entities.Country;
import com.im.admin.response.CommonResponse;
import com.im.admin.service.CountryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/country")
public class CountryController {

    @Autowired
    private CountryService countryService;

    @PostMapping
    public ResponseEntity<CommonResponse<Country>> create(@RequestBody Country country) {
        Country saved = countryService.createCountry(country);
        return ResponseEntity.ok(new CommonResponse<>(201, "Country added successfully", saved));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CommonResponse<Country>> getById(@PathVariable Long id) {
        return countryService.getCountryById(id)
                .map(country -> ResponseEntity.ok(new CommonResponse<>(200, "Country fetched", country)))
                .orElse(ResponseEntity.status(404).body(new CommonResponse<>(404, "Country not found", null)));
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
        Page<Country> filteredCountries = countryService.getFilteredCountries(page, size,
                id, country, countryCode, blocked, createdBy, editedBy, createdOn);

        return ResponseEntity.ok(new CommonResponse<>(200, "Countries fetched", filteredCountries));
    }


    @PutMapping("/{id}")
    public ResponseEntity<CommonResponse<Country>> update(@PathVariable Long id, @RequestBody Country country) {
        try {
            Country updated = countryService.updateCountry(id, country);
            return ResponseEntity.ok(new CommonResponse<>(200, "Country updated", updated));
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(new CommonResponse<>(404, e.getMessage(), null));
        }
    }

    @GetMapping("/dropdown")
    public ResponseEntity<CommonResponse<List<CountryDropdownDto>>> getCountryDropdown() {
        List<CountryDropdownDto> dropdownList = countryService.getCountryDropdown();
        return ResponseEntity.ok(new CommonResponse<>(200, "Country dropdown fetched", dropdownList));
    }

}
