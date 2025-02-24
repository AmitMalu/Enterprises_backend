package com.im.admin.dto;

import com.im.admin.entities.Address;
import com.im.admin.entities.Info;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DistributorDTO {
    private Long id;

    @NotBlank(message = "Parent account is mandatory")
    private String parentAccount;

    @Valid
    private Info info;

    private String competitor;

    @Valid
    private Address address;

    @Valid
    private Address outletAddress;
}

