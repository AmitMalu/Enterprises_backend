package com.im.admin.response;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OutletAddressDto {

    private String outletBuilding;
//    private String outletStreetName;
//    private String outletArea;
//    private String outletLocality;
    private String outletCity;
    private String outletState;
    private String outletPinCode;
}
