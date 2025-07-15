package com.im.admin.dto;


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
    private String stateName;
    private String districtName;
    private String cityName;
    private Boolean blocked;
    private LocalDateTime createdOn;
    private String createdBy;
    private LocalDateTime editedOn;
    private String editedBy;

}

