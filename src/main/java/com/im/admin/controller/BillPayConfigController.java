package com.im.admin.controller;

import com.im.admin.entities.BillPayConfig;
import com.im.admin.entities.VendorRouting;
import com.im.admin.entities.vendor.Vendors;
import com.im.admin.repositories.VendorRoutingRepository;
import com.im.admin.request.BillPayConfigDto;
import com.im.admin.response.CommonResponse;
import com.im.admin.service.BillPayConfigService;
import com.im.admin.util.CommonMethods;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/billpay/config")
public class BillPayConfigController {

    @Autowired
    private VendorRoutingRepository vendorRoutingRepository;

    @Autowired
    private BillPayConfigService service;

    @PostMapping("/add")
    public CommonResponse<BillPayConfig> addConfig(@RequestBody BillPayConfig config) {
        BillPayConfig saved = service.saveConfig(config);
        return CommonResponse.created(saved, "Config saved successfully");
    }

    @PostMapping("/services")
    public Map<String, Object> getServices(@RequestBody BillPayConfigDto req) {

        // STEP 1: Fetch services
        List<String> list = service.getServiceNamesBySubProduct(req.getSubProductId());

        // STEP 2: Fetch vendor
        VendorRouting routing = vendorRoutingRepository.findBySubProductId(req.getSubProductId());
        Vendors vendor = CommonMethods.getVendors(routing);

        // STEP 3: Prepare data list
        List<Map<String, String>> responseList = new ArrayList<>();
        for (String serviceName : list) {
            Map<String, String> map = new LinkedHashMap<>();
            map.put("serviceName", serviceName);
            responseList.add(map);
        }

        // STEP 4: Prepare final response using LinkedHashMap
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("statusCode", 200);
        response.put("message", "Service list fetched successfully");
        response.put("vendorName", vendor.getVendorName());
        response.put("data", responseList);

        return response;
    }


    @PostMapping("/providers")
    public CommonResponse<List<Map<String, Object>>> getProviders(@RequestBody BillPayConfigDto req) {

        List<Object[]> rows = service.getProviders(req.getSubProductId(), req.getServiceName());

        List<Map<String, Object>> result = new ArrayList<>();

        for (Object[] row : rows) {
            Map<String, Object> map = new HashMap<>();
            map.put("providerId", row[0]);
            map.put("providerName", row[1]);
            result.add(map);
        }

        return CommonResponse.success(result, "Providers fetched successfully");
    }

    @PostMapping("/raw-config")
    public CommonResponse<String> getRawConfig(@RequestBody BillPayConfigDto req) {

        String rawConfig = service.getRawConfig(
                req.getSubProductId(),
                req.getProviderId()
        );

        if (rawConfig == null) {
            return CommonResponse.failure("No raw config found for given provider");
        }

        return CommonResponse.success(rawConfig, "Raw config fetched successfully");
    }

}
