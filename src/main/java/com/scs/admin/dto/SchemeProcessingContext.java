package com.scs.admin.dto;

import com.scs.admin.entities.SchemeRate;
import com.scs.admin.entities.product.Products;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;

/**
 * @author SHUBHAM KHOPADE
 */
@Data
@AllArgsConstructor
public class SchemeProcessingContext {

    private TransactionSchemeDetailsRequestDto dto;

    private SchemeRate schemeRate;

    private Products product;
}
