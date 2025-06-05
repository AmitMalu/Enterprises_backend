package com.im.admin.util;


import java.math.BigDecimal;

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
}

