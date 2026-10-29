package com.scs.admin.entities;


import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Embeddable
public class Info {

    @NotBlank(message = "Title is mandatory")
    private String title;

    @NotBlank(message = "Name is mandatory")
    private String name;

    @NotBlank(message = "Mobile is mandatory")
    @Pattern(regexp = "^[0-9]{10}$", message = "Mobile number must be 10 digits")
    private String mobile;

    @NotBlank(message = "Email is mandatory")
    @Email(message = "Invalid email format")
    private String email;

    private LocalDate dob;

    @NotBlank(message = "Outlet Name is mandatory")
    private String outletName;

    @NotBlank(message = "Product Groups are mandatory")
    private String productGroups;

    private String forWhiteLabel;

    private String companyType;
}
