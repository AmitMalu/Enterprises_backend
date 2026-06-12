package com.im.admin.spec;

import com.im.admin.entities.AccessHierarchyPath;
import com.im.admin.entities.PrefundRequest;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class PrefundRequestSpecification {

    public static Specification<PrefundRequest> getFilter(
            Long id,
            String requestedBy,
            String mobileNumber,
            Double depositAmount,
            String status,
            LocalDate depositDate,
            LocalDate fromDate,
            LocalDate toDate,
            LocalDate createDateTime,
            String bankHolderName,
            String bankAccountName,
            String bankAccountNumber,
            String bankTranId,
            String paymentMode,
            String narration,
            String ipAddress,
            LocalDate approveOrRejectDate,
            String approveOrRejectTime,
            String remarks,
            String depositType
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (id != null) predicates.add(cb.equal(root.get("id"), id));

            if (requestedBy != null)
                predicates.add(cb.like(
                        cb.lower(root.get("requestedBy")),
                        "%" + requestedBy.toLowerCase() + "%"
                ));

            if (mobileNumber != null)
                predicates.add(cb.like(cb.lower(root.get("mobileNumber")), "%" + mobileNumber.toLowerCase() + "%"));

            if (depositAmount != null)
                predicates.add(cb.equal(root.get("depositAmount"), depositAmount));

            if (status != null)
                predicates.add(cb.like(cb.lower(root.get("status")), "%" + status.toLowerCase() + "%"));

            if (depositDate != null)
                predicates.add(cb.equal(root.get("depositDate"), depositDate));

            //  FROM – TO DATE FILTER (createDateTime)
            if (fromDate != null && toDate != null) {
                predicates.add(cb.between(
                        root.get("createDateTime"),
                        fromDate.atStartOfDay(),
                        toDate.atTime(23, 59, 59)
                ));
            }

            if (createDateTime != null)
                predicates.add(cb.between(
                        root.get("createDateTime"),
                        createDateTime.atStartOfDay(),
                        createDateTime.atTime(23, 59, 59)
                ));

            if (bankHolderName != null)
                predicates.add(cb.like(cb.lower(root.get("bankHolderName")), "%" + bankHolderName.toLowerCase() + "%"));

            if (bankAccountName != null)
                predicates.add(cb.like(cb.lower(root.get("bankAccountName")), "%" + bankAccountName.toLowerCase() + "%"));

            if (bankAccountNumber != null)
                predicates.add(cb.like(cb.lower(root.get("bankAccountNumber")), "%" + bankAccountNumber.toLowerCase() + "%"));

            if (bankTranId != null)
                predicates.add(cb.like(cb.lower(root.get("bankTranId")), "%" + bankTranId.toLowerCase() + "%"));

            if (paymentMode != null)
                predicates.add(cb.like(cb.lower(root.get("paymentMode")), "%" + paymentMode.toLowerCase() + "%"));

            if (narration != null)
                predicates.add(cb.like(cb.lower(root.get("narration")), "%" + narration.toLowerCase() + "%"));

            if (ipAddress != null)
                predicates.add(cb.like(cb.lower(root.get("ipAddress")), "%" + ipAddress.toLowerCase() + "%"));

            if (approveOrRejectDate != null)
                predicates.add(cb.equal(root.get("approveOrRejectDate"), approveOrRejectDate));

            if (approveOrRejectTime != null)
                predicates.add(cb.like(cb.lower(root.get("approveOrRejectTime")), "%" + approveOrRejectTime.toLowerCase() + "%"));

            if (remarks != null)
                predicates.add(cb.like(cb.lower(root.get("remarks")), "%" + remarks.toLowerCase() + "%"));

            if(depositType != null)
                predicates.add(cb.like(cb.lower(root.get("depositType")), "%" + depositType.toLowerCase() + "%" ));

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<PrefundRequest> hierarchyScope(
            String loginHierarchyId) {

        return (root, query, cb) -> {

            assert query != null;
            Subquery<String> subquery = query.subquery(String.class);

            Root<AccessHierarchyPath> path =
                    subquery.from(AccessHierarchyPath.class);

            subquery.select(
                    path.get("id")
                            .get("descendantId")
            );

            subquery.where(
                    cb.equal(
                            path.get("id")
                                    .get("ancestorId"),
                            loginHierarchyId
                    )
            );

            return root.get("requestedBy").in(subquery);
        };
    }
}
