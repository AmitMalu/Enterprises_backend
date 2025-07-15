package com.im.admin.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PinCodeRequestDTO {

    private String pinCode;
    private Long countryId;
    private Long stateId;
    private Long districtId;
    private Long cityId;
    private Boolean blocked;
    private String createdBy;
    private String editedBy;


}

