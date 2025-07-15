package com.im.admin.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DistrictRequestDTO {

    private String districtName;
    private String districtCode;
    private Long countryId;
    private Long stateId;
    private Boolean blocked;
    private String createdBy;
    private String editedBy;

}

