package com.scs.admin.dto;


import jakarta.persistence.Column;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PinCodeResponseDTO {

    private Long id;
    private String pinCode;
    private String countryName;
    private Long countryId;
    private String stateName;
    private Long stateId;
    private String districtName;
    private Long districtId;
    private String cityName;
    private Long cityId;
    private Boolean blocked;
    private LocalDateTime createdOn;
    private String createdBy;
    private LocalDateTime editedOn;
    private String editedBy;

}

