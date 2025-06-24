package com.im.admin.service;

import com.im.admin.response.RunPaisaCallback;

import java.util.Map;

public interface VendorCallbackParser {

    boolean supports(Map<String, String> payload);

    RunPaisaCallback parse(Map<String, String> payload);
}

