package com.im.admin.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StateRequestDTO {
    private String zone;
    private String stateName;
    private String stateCode;
    private String regionType;
    private Long countryId;
    private Boolean blocked;
    private String createdBy;
    private String editedBy;
}
