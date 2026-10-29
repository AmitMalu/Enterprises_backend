package com.scs.admin.service;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Summary returned after a bulk bank upload.
 *
 * @author SHUBHAM KHOPADE
 */
@Data
@Builder
public class BankBulkUploadResult {

    private int totalRows;
    private int inserted;
    private int skipped;      // duplicate bank name — already exists
    private int errorCount;
    private long elapsedMs;
    private List<String> errors;  // first 50 row-level error messages max
}
