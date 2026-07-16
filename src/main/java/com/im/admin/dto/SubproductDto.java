package com.im.admin.dto;

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

    private String subproduct;
}
