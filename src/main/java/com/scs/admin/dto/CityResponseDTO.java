package com.scs.admin.dto;


import jakarta.persistence.Column;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CityResponseDTO {

    private Long id;
    private String cityName;
    private String cityCode;
    private Long countryId;
    private String countryName;
    private Long stateId;
    private String stateName;
    private Long districtId;
    private String districtName;
    private Boolean blocked;
    private LocalDateTime createdOn;
    private String createdBy;
    private LocalDateTime editedOn;
    private String editedBy;

}

