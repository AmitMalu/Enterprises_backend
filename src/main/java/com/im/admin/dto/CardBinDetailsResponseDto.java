package com.im.admin.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CardBinDetailsResponseDto {

    private String cardType;
    private String issuerName;

}

