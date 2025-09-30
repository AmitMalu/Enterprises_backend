package com.im.admin.response;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MyProfileResponseDto {

    private InfoDto info;
    private AddressDto address;
    private OutletAddressDto outletAddress;
    private List<MyBanksDto> myBanksList;
    private KycDocsDto kycDocs;

}
