package com.im.admin.response;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MyBanksDto {

    private int srNo;
    private Long id;
    private String bankName;
    private String accountName;
    private String accountNo;
    private String IFCSCode;
    private Boolean verified;
}
