package com.im.admin.response;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MyProfileResponseDto {

    private InfoDto info;
    private AddressDto address;
    private OutletAddressDto outletAddress;
    private MyBanksDto myBanks;
    private KycDocsDto kycDocs;

}
