package com.im.admin.service;

import com.im.admin.request.DigilockerInitRequest;
import com.im.admin.response.DigilockerAadhaarResponse;
import com.im.admin.response.DigilockerInitResponse;

/**
 * @author SHUBHAM KHOPADE
 */
public interface DigilockerService {

    DigilockerInitResponse initializeDigilocker();

    DigilockerAadhaarResponse downloadAadhaar(String clientId);

}
