package com.im.admin.service;

import com.im.admin.dto.FetchSubProductFromRetailerRequest;
import com.im.admin.entities.SchemeAllocation;

public interface PaymentGatewayService {

    SchemeAllocation fetchSubProductFromRetailerId(FetchSubProductFromRetailerRequest fetchSubProductFromRetailerRequest);


}
