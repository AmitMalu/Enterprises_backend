package com.im.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CountryDropdownDto {
    private Long id;
    private String countryName;
}
