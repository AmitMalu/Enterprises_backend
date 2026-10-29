package com.scs.admin.repositories;

import com.scs.admin.request.HierarchyWalletReportRequest;
import com.scs.admin.response.HierarchyWalletReportDto;
import jakarta.persistence.EntityManager;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.StoredProcedureQuery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * author abdul.M
 */
@Repository
public class HierarchyWalletReportRepository {

    private final EntityManager entityManager;

    public HierarchyWalletReportRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    /**
     * Fetches wallet preview data from SP, filters by hierarchyIdsCsv, then paginates in Java.
     *
     * @param request         pagination + filter params
     * @param hierarchyIdsCsv comma-separated list of allowed hierarchy IDs (null = no filter = AD sees all)
     */
    public Page<HierarchyWalletReportDto> getReport(HierarchyWalletReportRequest request,
                                                     String hierarchyIdsCsv) {

        int pageNo   = request.getPageNo();
        int pageSize = request.getPageSize();

        // Build allowed ID set from CSV (null means no filter)
        Set<String> allowedIds = null;
        if (hierarchyIdsCsv != null && !hierarchyIdsCsv.isBlank()) {
            allowedIds = new HashSet<>(Arrays.asList(hierarchyIdsCsv.split(",")));
        }

        // Call SP — fetch all rows, filter in Java, then paginate manually
        StoredProcedureQuery query =
                entityManager.createStoredProcedureQuery("sp_hierarchy_wallet_report");

        query.registerStoredProcedureParameter("p_hierarchy_type", String.class,  ParameterMode.IN);
        query.registerStoredProcedureParameter("p_offset",         Integer.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("p_limit",          Integer.class, ParameterMode.IN);

        query.setParameter("p_hierarchy_type", request.getHierarchyType());
        query.setParameter("p_offset", 0);
        query.setParameter("p_limit",  Integer.MAX_VALUE); // fetch all; paginate after filtering

        @SuppressWarnings("unchecked")
        List<Object[]> result = query.getResultList();

        List<HierarchyWalletReportDto> response = new ArrayList<>();
        final Set<String> finalAllowedIds = allowedIds;

        for (Object[] row : result) {

            String hierarchyId = (String) row[1];

            // Skip rows not in the allowed set (network filter)
            if (finalAllowedIds != null && !finalAllowedIds.contains(hierarchyId)) {
                continue;
            }

            HierarchyWalletReportDto dto = new HierarchyWalletReportDto();
            dto.setHierarchyType((String) row[0]);
            dto.setHierarchyId(hierarchyId);
            dto.setFullName((String) row[2]);
            dto.setOutletCompanyName((String) row[3]);
            dto.setMobileNumber((String) row[4]);
            dto.setEmailId((String) row[5]);

            dto.setPrepaidBalance((BigDecimal) row[6]);
            dto.setPrepaidCutoffAmount((BigDecimal) row[7]);

            dto.setPostpaidBalance((BigDecimal) row[8]);
            dto.setPostpaidCutoffAmount((BigDecimal) row[9]);

            dto.setCreatedOn(row[10] != null ? ((Timestamp) row[10]).toLocalDateTime() : null);
            dto.setCreatedBy((String) row[11]);
            dto.setLastUpdatedDate(row[12] != null ? ((Date) row[12]).toLocalDate() : null);
            dto.setRemark((String) row[13]);

            response.add(dto);
        }

        // Manual pagination on filtered list
        long total = response.size();
        int  start = pageNo * pageSize;
        int  end   = Math.min(start + pageSize, response.size());

        List<HierarchyWalletReportDto> paged =
                start >= response.size() ? new ArrayList<>() : response.subList(start, end);

        return new PageImpl<>(paged, PageRequest.of(pageNo, pageSize), total);
    }
}
