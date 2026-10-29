package com.scs.admin.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;


public interface CommonSearchTransactionDetails {

    String getTransactionId();

    String getHierarchyId();   // alias for retailerId, distributorId, etc.

    BigDecimal getBalBeforeTran();

    BigDecimal getAmount();

    BigDecimal getBalAfterTran();

    String getSubProductName();

    String getNarration();

    String getTranStatus();

    LocalDate getDateOfTransaction();

    LocalTime getTimeOfTransaction();

    String getVendorName();
    String getVendorTransactionId();
    Double getLatitude();
    Double getLongitude();
}