package com.scs.admin.service;

import com.scs.admin.entities.BillPayConfig;

import java.util.List;

public interface BillPayConfigService {
    BillPayConfig saveConfig(BillPayConfig config);

    List<String> getServiceNamesBySubProduct(String subProductId);

    List<Object[]> getProviders(String subProductId, String serviceName);

    String getRawConfig(String subProductId, String providerId);
}
