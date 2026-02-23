package com.im.admin.service;

import java.math.BigDecimal;

public interface MonthlyTransactionVolumeProjection {
    String getMonth();   // yyyy-MM
    BigDecimal getValue();
}
