package com.scs.admin.service;

import com.scs.admin.entities.VendorCredentials;

public interface PayoutTokenProvider {

    String generateToken(VendorCredentials creds);

}
