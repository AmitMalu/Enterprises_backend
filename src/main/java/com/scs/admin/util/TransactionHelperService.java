package com.scs.admin.util;

import org.springframework.stereotype.Service;

/**
 * @author SHUBHAM KHOPADE
 */

@Service
public class TransactionHelperService {

    public String getHierarchyColumnName(String hierarchyType) {
        return switch (hierarchyType) {
            case "ET" -> "enterprises_id";
            case "PT" -> "partner_id";
            case "RT" -> "retailer_id";
            case "DT" -> "distributor_id";
            case "SD" -> "super_distributor_id";
            case "WL" -> "white_label_id";
            case "AD" -> "admin_id";
            default -> throw new IllegalArgumentException("Invalid hierarchy type: " + hierarchyType);
        };
    }
}
