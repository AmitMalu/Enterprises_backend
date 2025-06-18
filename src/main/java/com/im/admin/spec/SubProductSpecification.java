package com.im.admin.spec;

import com.im.admin.entities.SubProducts;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class SubProductSpecification {

    public static Specification<SubProducts> getFilter(
            List<String> id,
            String subProductTitle,
            String subProductCode,
            String productCode,
            Short productId,
            String productName,
            String walletType,
            Boolean isSettlement,
            Boolean isActive,
            Boolean enabled,
            String createdBy,
            String editedBy,
            LocalDate createdOn,
            LocalDate editedOn
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (id != null && !id.isEmpty()) {
                List<Predicate> idPredicates = new ArrayList<>();
                for (String val : id) {
                    idPredicates.add(cb.like(root.get("id"), "%" + val + "%"));
                }
                predicates.add(cb.or(idPredicates.toArray(new Predicate[0])));
            }

            if (subProductTitle != null)
                predicates.add(cb.like(cb.lower(root.get("subProductTitle")), "%" + subProductTitle.toLowerCase() + "%"));
            if (subProductCode != null)
                predicates.add(cb.like(cb.lower(root.get("subProductCode")), "%" + subProductCode.toLowerCase() + "%"));
            if (productCode != null)
                predicates.add(cb.like(cb.lower(root.get("productCode")), "%" + productCode.toLowerCase() + "%"));
            if (productId != null)
                predicates.add(cb.equal(root.get("productId"), productId));
            if (productName != null)
                predicates.add(cb.like(cb.lower(root.get("productName")), "%" + productName.toLowerCase() + "%"));
            if (walletType != null)
                predicates.add(cb.like(cb.lower(root.get("walletType")), "%" + walletType.toLowerCase() + "%"));
            if (isSettlement != null)
                predicates.add(cb.equal(root.get("isSettlement"), isSettlement));
            if (isActive != null)
                predicates.add(cb.equal(root.get("isActive"), isActive));
            if (enabled != null)
                predicates.add(cb.equal(root.get("enabled"), enabled));
            if (createdBy != null)
                predicates.add(cb.like(cb.lower(root.get("createdBy")), "%" + createdBy.toLowerCase() + "%"));
            if (editedBy != null)
                predicates.add(cb.like(cb.lower(root.get("editedBy")), "%" + editedBy.toLowerCase() + "%"));

            if (createdOn != null) {
                predicates.add(cb.between(
                        root.get("createdOn"),
                        createdOn.atStartOfDay(),
                        createdOn.atTime(23, 59, 59)
                ));
            }

            if (editedOn != null) {
                predicates.add(cb.between(
                        root.get("editedOn"),
                        editedOn.atStartOfDay(),
                        editedOn.atTime(23, 59, 59)
                ));
            }


            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
