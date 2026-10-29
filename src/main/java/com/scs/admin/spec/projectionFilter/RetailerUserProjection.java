package com.scs.admin.spec.projectionFilter;


import java.time.LocalDateTime;

public interface RetailerUserProjection {

    String getRetailerUserId();
    String getName();
    String getMobileNumber();
    String getEmailId();
    Boolean getEmailVerified();
    Boolean getStatus();
    LocalDateTime getCreatedOn();
    String getCreatedBy();
    LocalDateTime getLastEditedOn();
    String getEditedBy();
}


