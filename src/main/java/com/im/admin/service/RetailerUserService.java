package com.im.admin.service;

import com.im.admin.entities.RetailerUser;
import com.im.admin.response.CommonResponse;
import com.im.admin.spec.projectionFilter.RetailerUserProjection;

import java.util.List;
import java.util.Map;

public interface RetailerUserService {

    RetailerUser createRetailerUser(RetailerUser retailerUser);

    CommonResponse<Map<String, Object>> getFilteredUsers(
            String name,
            String mobileNumber,
            String emailId,
            Boolean emailVerified,
            Boolean status,
            int page,
            int size,
            String sortBy,
            String sortDir
    );

    CommonResponse<Void> deleteRetailerUser(String id);

    RetailerUser updateRetailerUser(String retailerUserId, RetailerUser retailerUser);


}
