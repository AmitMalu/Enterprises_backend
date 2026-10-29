package com.scs.admin.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class InfoDto {

    private String name;
    private String memberId;
    private String mobileNumber;
    private String email;
    private String dob;
    private String outletName;
    private Boolean isBlackList;
    private Boolean isBlocked;
    private String status;
}
