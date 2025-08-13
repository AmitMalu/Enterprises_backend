package com.im.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class BbpsVendorWrapperDTO {

    private Integer status;
    private String message;
    private List<BbpsVendorRawDTO> providerList;
}

