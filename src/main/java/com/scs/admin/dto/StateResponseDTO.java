package com.scs.admin.dto;

import jakarta.persistence.Column;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class StateResponseDTO {
    private Long id;
    private String zone;
    private String stateName;
    private String stateCode;
    private String regionType;
    private String countryName;
    private Long countryId;
    private Boolean blocked;
    private String createdBy;
    private String editedBy;
    private LocalDateTime createdOn;
    private LocalDateTime editedOn;
}

