package com.im.admin.entities;

import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "super_distributor")
public class SuperDistributor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

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

//    @Embedded
//    @Valid
//    private Info info;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "address", column = @Column(name = "main_address")),
            @AttributeOverride(name = "city", column = @Column(name = "main_city")),
            @AttributeOverride(name = "state", column = @Column(name = "main_state")),
            @AttributeOverride(name = "zipCode", column = @Column(name = "main_zip_code"))
    })
    @Valid
    private Address address;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "address", column = @Column(name = "outlet_address")),
            @AttributeOverride(name = "city", column = @Column(name = "outlet_city")),
            @AttributeOverride(name = "state", column = @Column(name = "outlet_state")),
            @AttributeOverride(name = "zipCode", column = @Column(name = "outlet_zip_code"))
    })
    @Valid
    private Address outletAddress;

}