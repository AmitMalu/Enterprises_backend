package com.scs.admin.service;

import com.scs.admin.request.UpdateLienAmountRequest;
import com.scs.admin.response.HierarchyWalletReportDto;

/**
 * Service for hierarchy wallet detail and lien-amount update operations.
 */
public interface HierarchyWalletService {

    /**
     * Fetch full wallet details for a single hierarchy record.
     * The hierarchy type is derived automatically from the ID format (e.g. BZT-RT-001 → RT).
     *
     * @param hierarchyId the unique ID of the hierarchy member
     * @return wallet details DTO
     */
    HierarchyWalletReportDto getWalletDetails(String hierarchyId);

    /**
     * Update the lien (cutoff) amounts for the given hierarchy member.
     * Validates that each cutoff amount does not exceed the current balance.
     *
     * @param request contains hierarchyType, hierarchyId, prepaidCutoffAmount,
     *                postpaidCutoffAmount and optional remarks
     * @return updated wallet details
     */
    HierarchyWalletReportDto updateLienAmount(UpdateLienAmountRequest request);
}
