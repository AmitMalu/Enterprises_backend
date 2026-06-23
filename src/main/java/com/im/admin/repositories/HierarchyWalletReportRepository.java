package com.im.admin.repositories;

import com.im.admin.request.HierarchyWalletReportRequest;
import com.im.admin.response.HierarchyWalletReportDto;
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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * author abdul.M
 */
@Repository
public class HierarchyWalletReportRepository {

    private final EntityManager entityManager;

    public HierarchyWalletReportRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public Page<HierarchyWalletReportDto> getReport(HierarchyWalletReportRequest request) {

        int pageNo   = request.getPageNo();
        int pageSize = request.getPageSize();

        StoredProcedureQuery query =
                entityManager.createStoredProcedureQuery("sp_hierarchy_wallet_report");

        query.registerStoredProcedureParameter("p_hierarchy_type", String.class,  ParameterMode.IN);
        query.registerStoredProcedureParameter("p_offset",         Integer.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("p_limit",          Integer.class, ParameterMode.IN);

        query.setParameter("p_hierarchy_type", request.getHierarchyType());
        query.setParameter("p_offset",         pageNo * pageSize);
        query.setParameter("p_limit",          pageSize);

        List<Object[]> result = query.getResultList();

        List<HierarchyWalletReportDto> response = new ArrayList<>();

        for (Object[] row : result) {
            HierarchyWalletReportDto dto = new HierarchyWalletReportDto();

            dto.setHierarchyType((String) row[0]);
            dto.setHierarchyId((String) row[1]);
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

            // row[12] = pp.last_updated_date (DATE from prepaid table)
            dto.setLastUpdatedDate(row[12] != null ? ((Date) row[12]).toLocalDate() : null);

            dto.setRemark((String) row[13]);

            response.add(dto);
        }

        long total = (pageNo == 0 && response.size() < pageSize)
                ? response.size()
                : (long) (pageNo + 1) * pageSize + (response.size() < pageSize ? response.size() : pageSize);

        return new PageImpl<>(response, PageRequest.of(pageNo, pageSize), total);
    }
}
