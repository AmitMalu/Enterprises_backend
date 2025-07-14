package com.im.admin.service.impl;

import com.im.admin.dto.CountryDropdownDto;
import com.im.admin.entities.Country;
import com.im.admin.repositories.CountryRepository;
import com.im.admin.service.CountryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class CountryServiceImpl implements CountryService {

    @Autowired
    CountryRepository countryRepository;

    @Override
    public Country createCountry(Country country) {
        return countryRepository.save(country);
    }

    @Override
    public Optional<Country> getCountryById(Long id) {
        return countryRepository.findById(id);
    }

    @Override
    public Page<Country> getFilteredCountries(int page, int size,
                                              Long id, String country, String countryCode, Boolean blocked,
                                              String createdBy, String editedBy, String createdOnStr) {
        List<Country> allCountries = countryRepository.findAll();

        List<Country> filtered = allCountries.stream()
                .filter(c -> id == null || c.getId().equals(id))
                .filter(c -> country == null || (c.getCountryName() != null && c.getCountryName().toLowerCase().contains(country.toLowerCase())))
                .filter(c -> countryCode == null || (c.getCountryCode() != null && c.getCountryCode().toLowerCase().contains(countryCode.toLowerCase())))
                .filter(c -> blocked == null || (c.getBlocked() != null && c.getBlocked().equals(blocked)))
                .filter(c -> createdBy == null || (c.getCreatedBy() != null && c.getCreatedBy().toLowerCase().contains(createdBy.toLowerCase())))
                .filter(c -> editedBy == null || (c.getEditedBy() != null && c.getEditedBy().toLowerCase().contains(editedBy.toLowerCase())))
                .collect(Collectors.toList());

        // Manual pagination
        int start = page * size;
        int end = Math.min(start + size, filtered.size());

        List<Country> pagedList = (start < end) ? filtered.subList(start, end) : List.of();
        return new PageImpl<>(pagedList, PageRequest.of(page, size), filtered.size());
    }


    @Override
    public Country updateCountry(Long id, Country updatedCountry) {
        Country existing = countryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Country not found"));

        existing.setCountryName(updatedCountry.getCountryName());
        existing.setCountryCode(updatedCountry.getCountryCode());
        existing.setBlocked(updatedCountry.getBlocked());
        existing.setEditedOn(LocalDateTime.now());
        existing.setEditedBy(updatedCountry.getEditedBy());

        return countryRepository.save(existing);
    }

    @Override
    public List<CountryDropdownDto> getCountryDropdown() {
        return countryRepository.findAll().stream()
                .map(c -> new CountryDropdownDto(c.getId(), c.getCountryName()))
                .collect(Collectors.toList());
    }

}
