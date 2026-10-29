package com.scs.admin.spec;


import com.scs.admin.entities.AccessHierarchyPath;
import com.scs.admin.entities.PrefundRequest;
import com.scs.admin.util.CommonMethods;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public final class PrefundRequestSpecification {

    private PrefundRequestSpecification() {
    }

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

            List<jakarta.persistence.criteria.Predicate> predicates =
                    new ArrayList<>();

            if (id != null) {
                predicates.add(
                        cb.equal(root.get("id"), id)
                );
            }

            if (CommonMethods.hasText(requestedBy)) {
                predicates.add(
                        cb.like(
                                cb.lower(root.get("requestedBy")),
                                "%" + requestedBy.trim()
                                        .toLowerCase() + "%"
                        )
                );
            }

            if (CommonMethods.hasText(mobileNumber)) {
                predicates.add(
                        cb.like(
                                cb.lower(root.get("mobileNumber")),
                                "%" + mobileNumber.trim()
                                        .toLowerCase() + "%"
                        )
                );
            }

            if (depositAmount != null) {
                predicates.add(
                        cb.equal(
                                root.get("depositAmount"),
                                depositAmount
                        )
                );
            }

            if (CommonMethods.hasText(status)) {
                predicates.add(
                        cb.equal(
                                cb.lower(root.get("status")),
                                status.trim().toLowerCase()
                        )
                );
            }

            if (depositDate != null) {
                predicates.add(
                        cb.equal(
                                root.get("depositDate"),
                                depositDate
                        )
                );
            }

            /*
             * Date range filter on createDateTime.
             *
             * Example:
             * fromDate = 2026-07-25
             * toDate   = 2026-07-25
             *
             * SQL-equivalent condition:
             * create_date_time >= 2026-07-25 00:00:00
             * create_date_time <  2026-07-26 00:00:00
             */
            if (fromDate != null) {

                LocalDateTime fromDateTime =
                        fromDate.atStartOfDay();

                predicates.add(
                        cb.greaterThanOrEqualTo(
                                root.get("createDateTime"),
                                fromDateTime
                        )
                );
            }

            if (toDate != null) {

                LocalDateTime toDateTimeExclusive =
                        toDate.plusDays(1).atStartOfDay();

                predicates.add(
                        cb.lessThan(
                                root.get("createDateTime"),
                                toDateTimeExclusive
                        )
                );
            }

            /*
             * Exact single-date filter.
             *
             * Do not use:
             * root.get("createDateTime").as(LocalDate.class)
             */
            if (createDateTime != null) {

                LocalDateTime dayStart =
                        createDateTime.atStartOfDay();

                LocalDateTime nextDayStart =
                        createDateTime
                                .plusDays(1)
                                .atStartOfDay();

                predicates.add(
                        cb.greaterThanOrEqualTo(
                                root.get("createDateTime"),
                                dayStart
                        )
                );

                predicates.add(
                        cb.lessThan(
                                root.get("createDateTime"),
                                nextDayStart
                        )
                );
            }

            if (CommonMethods.hasText(bankHolderName)) {
                predicates.add(
                        cb.like(
                                cb.lower(root.get("bankHolderName")),
                                "%" + bankHolderName.trim()
                                        .toLowerCase() + "%"
                        )
                );
            }

            if (CommonMethods.hasText(bankAccountName)) {
                predicates.add(
                        cb.like(
                                cb.lower(root.get("bankAccountName")),
                                "%" + bankAccountName.trim()
                                        .toLowerCase() + "%"
                        )
                );
            }

            if (CommonMethods.hasText(bankAccountNumber)) {
                predicates.add(
                        cb.like(
                                cb.lower(root.get("bankAccountNumber")),
                                "%" + bankAccountNumber.trim()
                                        .toLowerCase() + "%"
                        )
                );
            }

            if (CommonMethods.hasText(bankTranId)) {
                predicates.add(
                        cb.like(
                                cb.lower(root.get("bankTranId")),
                                "%" + bankTranId.trim()
                                        .toLowerCase() + "%"
                        )
                );
            }

            if (CommonMethods.hasText(paymentMode)) {
                predicates.add(
                        cb.equal(
                                cb.lower(root.get("paymentMode")),
                                paymentMode.trim().toLowerCase()
                        )
                );
            }

            if (CommonMethods.hasText(narration)) {
                predicates.add(
                        cb.like(
                                cb.lower(root.get("narration")),
                                "%" + narration.trim()
                                        .toLowerCase() + "%"
                        )
                );
            }

            if (CommonMethods.hasText(ipAddress)) {
                predicates.add(
                        cb.like(
                                cb.lower(root.get("ipAddress")),
                                "%" + ipAddress.trim()
                                        .toLowerCase() + "%"
                        )
                );
            }

            if (approveOrRejectDate != null) {
                predicates.add(
                        cb.equal(
                                root.get("approveOrRejectDate"),
                                approveOrRejectDate
                        )
                );
            }

            if (CommonMethods.hasText(approveOrRejectTime)) {
                predicates.add(
                        cb.like(
                                cb.lower(
                                        root.get("approveOrRejectTime")
                                ),
                                "%" + approveOrRejectTime.trim()
                                        .toLowerCase() + "%"
                        )
                );
            }

            if (CommonMethods.hasText(remarks)) {
                predicates.add(
                        cb.like(
                                cb.lower(root.get("remarks")),
                                "%" + remarks.trim()
                                        .toLowerCase() + "%"
                        )
                );
            }

            if (CommonMethods.hasText(depositType)) {
                predicates.add(
                        cb.equal(
                                cb.lower(root.get("depositType")),
                                depositType.trim().toLowerCase()
                        )
                );
            }

            return cb.and(
                    predicates.toArray(
                            new jakarta.persistence.criteria.Predicate[0]
                    )
            );
        };
    }

    /**
     * Fetches requests raised by:
     *
     * 1. The effective hierarchy itself because depth=0 exists.
     * 2. Every descendant below the effective hierarchy.
     */
    public static Specification<PrefundRequest> hierarchyScope(
            String effectiveHierarchyId
    ) {

        return (root, query, cb) -> {

            if (!CommonMethods.hasText(effectiveHierarchyId)) {
                throw new IllegalArgumentException(
                        "Effective hierarchy ID cannot be empty"
                );
            }

            Subquery<String> descendantSubquery =
                    query.subquery(String.class);

            Root<AccessHierarchyPath> hierarchyPath =
                    descendantSubquery.from(
                            AccessHierarchyPath.class
                    );

            Path<String> ancestorId =
                    hierarchyPath
                            .get("id")
                            .get("ancestorId");

            Path<String> descendantId =
                    hierarchyPath
                            .get("id")
                            .get("descendantId");

            descendantSubquery.select(descendantId);

            descendantSubquery.where(
                    cb.equal(
                            ancestorId,
                            effectiveHierarchyId.trim()
                    )
            );

            return root
                    .get("requestedBy")
                    .in(descendantSubquery);
        };
    }
}
