package com.im.admin.service.impl;

import com.im.admin.dto.CountryDropdownDto;
import com.im.admin.entities.Country;
import com.im.admin.repositories.CountryRepository;
import com.im.admin.service.CountryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CountryServiceImpl implements CountryService {

    @Autowired
    CountryRepository countryRepository;

    @Override
    public Country createCountry(Country country) {
        log.info("Creating country: {}", country);
        Country savedCountry = countryRepository.save(country);
        log.info("Country created successfully with ID: {}", savedCountry.getId());
        return savedCountry;
    }

    @Override
    public Optional<Country> getCountryById(Long id) {
        log.info("Fetching country by ID: {}", id);
        Optional<Country> country = countryRepository.findById(id);
        if (country.isPresent()) {
            log.info("Country found: {}", country.get());
        } else {
            log.warn("No country found with ID: {}", id);
        }
        return country;
    }


    @Override
    public Page<Country> getFilteredCountries(int page, int size,
                                              Long id, String country, String countryCode, Boolean blocked,
                                              String createdBy, String editedBy, String createdOnStr) {
        log.info("Fetching filtered countries with params - page: {}, size: {}, id: {}, country: {}, countryCode: {}, blocked: {}, createdBy: {}, editedBy: {}, createdOnStr: {}",
                page, size, id, country, countryCode, blocked, createdBy, editedBy, createdOnStr);

        List<Country> allCountries = countryRepository.findAll();
        log.info("Total countries fetched from DB: {}", allCountries.size());

        List<Country> filtered = allCountries.stream()
                .filter(c -> id == null || c.getId().equals(id))
                .filter(c -> country == null || (c.getCountryName() != null && c.getCountryName().toLowerCase().contains(country.toLowerCase())))
                .filter(c -> countryCode == null || (c.getCountryCode() != null && c.getCountryCode().toLowerCase().contains(countryCode.toLowerCase())))
                .filter(c -> blocked == null || (c.getBlocked() != null && c.getBlocked().equals(blocked)))
                .filter(c -> createdBy == null || (c.getCreatedBy() != null && c.getCreatedBy().toLowerCase().contains(createdBy.toLowerCase())))
                .filter(c -> editedBy == null || (c.getEditedBy() != null && c.getEditedBy().toLowerCase().contains(editedBy.toLowerCase())))
                .sorted(Comparator.comparing(Country::getCreatedOn).reversed())
                .collect(Collectors.toList());

        log.info("Filtered countries count after applying filters: {}", filtered.size());

        // Manual pagination
        int start = page * size;
        int end = Math.min(start + size, filtered.size());
        log.info("Pagination applied - start: {}, end: {}", start, end);

        List<Country> pagedList = (start < end) ? filtered.subList(start, end) : List.of();
        log.info("Returning {} countries in this page", pagedList.size());

        return new PageImpl<>(pagedList, PageRequest.of(page, size), filtered.size());
    }



    @Override
    public Country updateCountry(Long id, Country updatedCountry) {
        log.info("Updating country with id: {}", id);

        Country existing = countryRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Country not found with id: {}", id);
                    return new RuntimeException("Country not found");
                });

        log.debug("Existing country before update: {}", existing);

        existing.setCountryName(updatedCountry.getCountryName());
        existing.setCountryCode(updatedCountry.getCountryCode());
        existing.setBlocked(updatedCountry.getBlocked());
        existing.setEditedOn(LocalDateTime.now());
        existing.setEditedBy(updatedCountry.getEditedBy());

        log.debug("Updated country details: {}", existing);

        Country savedCountry = countryRepository.save(existing);
        log.info("Country updated successfully with id: {}", savedCountry.getId());

        return savedCountry;
    }

    @Override
    public List<CountryDropdownDto> getCountryDropdown() {
        log.info("Fetching country dropdown list");

        List<CountryDropdownDto> dropdownList = countryRepository.findAll().stream()
                .map(c -> new CountryDropdownDto(c.getId(), c.getCountryName()))
                .collect(Collectors.toList());

        log.debug("Country dropdown list fetched: {}", dropdownList);
        log.info("Total countries in dropdown: {}", dropdownList.size());

        return dropdownList;
    }


}
