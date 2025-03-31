package com.im.admin.dto;


import com.im.admin.entities.SchemeRate;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TransactionSchemeDetailsResponseDto {

    private SchemeRate matchedSchemeRate;
    private String walletType;   //Prepaid, Postpaid, etc
    private String taxValue;    // gst, tds, etc

}
