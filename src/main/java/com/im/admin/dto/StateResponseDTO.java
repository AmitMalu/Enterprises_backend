package com.im.admin.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StateResponseDTO {
    private Long id;
    private String zone;
    private String stateName;
    private String stateCode;
    private String regionType;
    private String countryName;
    private Boolean blocked;
}
