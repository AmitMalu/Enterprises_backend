package com.im.admin.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VendorStateDTO {

    @JsonProperty("description")
    private String stateName;

    @JsonProperty("code")
    private String stateCode;

}

