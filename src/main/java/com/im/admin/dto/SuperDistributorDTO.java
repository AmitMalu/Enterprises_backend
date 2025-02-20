package com.im.admin.dto;

import com.im.admin.entities.Address;
import com.im.admin.entities.Info;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SuperDistributorDTO {

    private Long id;

    @Valid
    private Info info;

    @Valid
    private Address address;

    @Valid
    private Address outletAddress;
}

