package com.im.admin.service;

import com.im.admin.dto.RemitterEkycRequest;
import com.im.admin.dto.RemitterRegisterRequest;

public interface RemitterService {
    Object checkRemitter(String mobile);

    Object registerRemitter(RemitterRegisterRequest request);

    Object doEkyc(RemitterEkycRequest request);

}
