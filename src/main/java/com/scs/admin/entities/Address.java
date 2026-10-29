package com.scs.admin.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Address {

    @NotBlank(message = "Outlet Name is mandatory")
    @Column(name = "outlet_name")
    private String outletName;

    @NotBlank(message = "Address is mandatory")
    private String address;

    @NotBlank(message = "City is mandatory")
    private String city;

    @NotBlank(message = "State is mandatory")
    private String state;

    @NotBlank(message = "Zip Code is mandatory")
    private String zipCode;

    @NotBlank(message = "Company Type is mandatory")
    @Column(name = "company_type")
    private String companyType;

    @NotBlank(message = "Company Name is mandatory")
    @Column(name = "company_name")
    private String companyName;
}
