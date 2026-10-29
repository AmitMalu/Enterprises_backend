package com.scs.admin.dto;

import lombok.*;

/**
 * @author SHUBHAM KHOPADE
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RetailersExtraColumnsDto {

    private String medium;
    private String clientIp;
    private String requestDomainName;
    private Double latitude;
    private Double longitude;
}
