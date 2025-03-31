package com.im.admin.dto;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
public class CardTypeRequestDto {

//    private String productCode;

    @NotBlank(message = "Card type is required")
    private String cardType;

    @NotBlank(message = "Card BIN number is required")
    @Size(min = 6, max = 10, message = "Card BIN number must be between 6 and 10 digits")
    private String cardBIN_No;

}
