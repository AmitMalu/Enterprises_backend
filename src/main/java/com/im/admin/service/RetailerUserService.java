package com.im.admin.service;

import com.im.admin.entities.RetailerUser;
import com.im.admin.response.CommonResponse;
import com.im.admin.response.RetailerUserResponseDTO;
import com.im.admin.spec.projectionFilter.RetailerUserProjection;
import org.springframework.data.domain.Page;

import java.time.LocalDate;
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


    CommonResponse<Page<RetailerUserResponseDTO>> getRetailerUsersByRetailerId(String retailerId, String retailerUserId, String abbreviation, String name, String mobileNumber, String username, String emailId, Boolean emailVerified, Boolean status, String createdBy, String editedBy, String roleId, String subRoleId, LocalDate createdOn, int page, int size);
}
