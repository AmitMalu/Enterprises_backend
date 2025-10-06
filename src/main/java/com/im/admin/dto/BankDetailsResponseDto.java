package com.im.admin.dto;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class BankDetailsResponseDto {
    private Long id;
    private String bankName;
    private String bankHolderName;
    private String bankAccountNumber;
    private String ifscCode;
    private String concatenatedString;

    public BankDetailsResponseDto(Long id, String bankName, String bankHolderName, String bankAccountNumber,
                                  String ifscCode) {
        this.id = id;
        this.bankName = bankName;
        this.bankHolderName = bankHolderName;
        this.bankAccountNumber = bankAccountNumber;
        this.ifscCode = ifscCode;
        this.concatenatedString = bankHolderName +"/"+ bankAccountNumber +"/"+ bankName +"/"+ ifscCode;

    }

}

