package com.im.admin.dto;

import com.im.admin.entities.Address;
import com.im.admin.entities.Info;
import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class RetailerDTO {

    private Long id;
    private String parentAccount;
    private Info info;
    private String mccCategory;
    private String ormsId;
    private String competitor;
    private Address address;
    private Address outletAddress;
    private String district;

}

