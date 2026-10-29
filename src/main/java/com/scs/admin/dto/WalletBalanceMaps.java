package com.scs.admin.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * @author SHUBHAM KHOPADE
 */
@Data
public class WalletBalanceMaps {

    Map<String, BigDecimal> prepaid = new HashMap<>();
    Map<String, BigDecimal> postpaid = new HashMap<>();

}
