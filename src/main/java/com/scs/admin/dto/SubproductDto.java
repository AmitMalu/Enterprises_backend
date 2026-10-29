package com.scs.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author SHUBHAM KHOPADE
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubproductDto {

    private String title;

    /*
     * Database pg_code will be returned in this field.
     */
    private String subproduct;
}
