package com.scs.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AddressDTO {

    private String outletName;
    private String companyType;
    private String companyName;
    private String address;
    private String city;
    private String state;
    private String zipCode;

}

