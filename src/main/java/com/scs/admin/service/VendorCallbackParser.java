package com.scs.admin.service;

import com.scs.admin.response.RunPaisaCallback;

import java.util.Map;

public interface VendorCallbackParser {

    boolean supports(Map<String, String> payload);

    RunPaisaCallback parse(Map<String, String> payload);
}

