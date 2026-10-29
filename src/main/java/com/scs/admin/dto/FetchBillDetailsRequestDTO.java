package com.scs.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class FetchBillDetailsRequestDTO {

    @NotNull(message = "Provider ID is required")
    private Integer provider;

    @NotBlank(message = "Biller Number is required")
    private String billerNumber;

    private String dob; // Optional or Mandatory as per provider
    private String field_one; // Optional or Mandatory
    private String field_two; // Optional or Mandatory
    private String field_three; // Optional or Mandatory
    private String field_four; // Optional or Mandatory

    @NotBlank(message = "Customer Mobile Number is required")
    private String customerMobileNumber;

    private String subProductId;

}

