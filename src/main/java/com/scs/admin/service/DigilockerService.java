package com.scs.admin.service;

import com.scs.admin.request.DigilockerInitRequest;
import com.scs.admin.response.DigilockerAadhaarResponse;
import com.scs.admin.response.DigilockerInitResponse;

/**
 * @author SHUBHAM KHOPADE
 */
public interface DigilockerService {

    DigilockerInitResponse initializeDigilocker();

    DigilockerAadhaarResponse downloadAadhaar(String clientId);

}
