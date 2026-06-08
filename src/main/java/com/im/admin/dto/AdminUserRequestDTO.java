    package com.im.admin.dto;

    import com.fasterxml.jackson.annotation.JsonFormat;
    import jakarta.persistence.Column;
    import jakarta.validation.constraints.Email;
    import jakarta.validation.constraints.NotBlank;
    import jakarta.validation.constraints.Pattern;
    import jakarta.validation.constraints.Size;
    import lombok.Data;

    import java.time.LocalDate;
    import java.util.List;

    @Data
    public class AdminUserRequestDTO {


        @NotBlank(message = "Parent account is required")
        private String parentAccount;

        private String id;

        @Column(name = "abbreviation", length = 3)
        @Size(min = 2, max = 3, message = "Abbreviation must be between 2 and 3 characters")
        @Pattern(regexp = "^[A-Z]{2,3}$", message = "Abbreviation must be 2 to 3 uppercase letters (no numbers or special characters)")
        private String abbreviation;

        @NotBlank(message = "Name is mandatory")
        private String name;

        @NotBlank(message = "Mobile Number is mandatory")
        @Pattern(regexp = "^[0-9]{10}$", message = "Mobile number must be 10 digits")
        private String mobileNumber;

        private String username;

        private String password;

        @NotBlank(message = "Email is mandatory")
        @Email(message = "Invalid email format")
        private String emailId;

        private Boolean emailVerified;

        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate dob;

        private AddressDTO add;

        private String createdBy;

        private String editedBy;

        private String createdByIPAddress;

        private String roleId;

        private String subRoleId;

        private List<String> permissions;
    }
