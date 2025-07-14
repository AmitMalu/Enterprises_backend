package com.im.admin.service;

import com.im.admin.dto.CountryDropdownDto;
import com.im.admin.entities.Country;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface CountryService {
    Country createCountry(Country country);


    Optional<Country> getCountryById(Long id);


    Country updateCountry(Long id, Country updatedCountry);

    Page<Country> getFilteredCountries(int page, int size, Long id, String country, String countryCode, Boolean blocked, String createdBy, String editedBy, String createdOn);

    List<CountryDropdownDto> getCountryDropdown();
}
