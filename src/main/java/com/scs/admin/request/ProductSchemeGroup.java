package com.scs.admin.request;

/**
 * @author SHUBHAM KHOPADE
 */
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductSchemeGroup {
    private String productCode;
    private String productName;
    private List<SchemeGroup> schemes;
}
