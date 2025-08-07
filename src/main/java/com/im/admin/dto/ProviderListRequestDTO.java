package com.im.admin.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProviderListRequestDTO {

    private String api_token;
    private String userId;
    private String secretKey;
    private String subProductId;

}
