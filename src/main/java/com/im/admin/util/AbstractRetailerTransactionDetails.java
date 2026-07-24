package com.im.admin.util;


import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

public interface AbstractRetailerTransactionDetails {
    String getRetailerId();
    BigDecimal getAmount();
    String getOperatorName();
    String getOperatorCode();
    String getProductCode();
    String getSubProductCode();
    String getTranStatus();
    String getTransactionId();

    String getSubProductName();
    String getSchemeName();

    String getService();
    Double getLatitude();
    Double getLongitude();

    LocalDate getDateOfTransaction();

    LocalTime getTimeOfTransaction();

    String getMobileNo();

    String getBankRefId();

    String getRemarks();
}

