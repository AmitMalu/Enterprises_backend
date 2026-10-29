package com.scs.admin.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author SHUBHAM KHOPADE
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SchemeRateRequest {

    private String schemeCode;
    private Double amount;

}
