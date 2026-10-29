package com.scs.admin.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author SHUBHAM KHOPADE
 */

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ApiPanelRemitterRequest {

    private String mobileNo;
    private String productId;
    private String apiPartnerId;
}
