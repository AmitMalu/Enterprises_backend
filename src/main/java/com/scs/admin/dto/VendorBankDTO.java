package com.scs.admin.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VendorBankDTO {

    @JsonProperty("description")
    private String bankName;

    @JsonProperty("code")
    private String bankCode;

}

