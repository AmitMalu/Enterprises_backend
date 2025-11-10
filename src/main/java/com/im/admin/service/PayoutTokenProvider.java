package com.im.admin.service;

import com.im.admin.entities.VendorCredentials;

public interface PayoutTokenProvider {

    String generateToken(VendorCredentials creds);

}
