package com.scs.admin.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author SHUBHAM KHOPADE
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class BankResponseDto {
    private String bankName;
    private String accountNumber;

}
