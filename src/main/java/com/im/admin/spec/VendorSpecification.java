package com.im.admin.spec;


import com.im.admin.entities.vendor.Vendors;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class VendorSpecification {
    public static Specification<Vendors> getFilter(
            List<String> id,
            String vendorName,
            String vendorCode,
            String productId,
            String productCode,
            String subProductId,
            String subProductCode,
            String modesSupported,
            Boolean isActive,
            String bankType,
            String website,
            String logo,
            String parameter,
            String createdBy,
            String editedBy,
            LocalDate createdOn,
            LocalDate editedOn
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (id != null && !id.isEmpty()) {
                List<Predicate> idPreds = new ArrayList<>();
                for (String val : id) {
                    idPreds.add(cb.like(root.get("id").as(String.class), "%" + val + "%"));
                }
                predicates.add(cb.or(idPreds.toArray(new Predicate[0])));
            }

            if (vendorName != null)
                predicates.add(cb.like(cb.lower(root.get("vendorName")), "%" + vendorName.toLowerCase() + "%"));
            if (vendorCode != null)
                predicates.add(cb.like(cb.lower(root.get("vendorCode")), "%" + vendorCode.toLowerCase() + "%"));
            if (productId != null)
                predicates.add(cb.like(cb.lower(root.get("productId")), "%" + productId.toLowerCase() + "%"));
            if (productCode != null)
                predicates.add(cb.like(cb.lower(root.get("productCode")), "%" + productCode.toLowerCase() + "%"));
            if (subProductId != null)
                predicates.add(cb.like(cb.lower(root.get("subProductId")), "%" + subProductId.toLowerCase() + "%"));
            if (subProductCode != null)
                predicates.add(cb.like(cb.lower(root.get("subProductCode")), "%" + subProductCode.toLowerCase() + "%"));
            if (modesSupported != null)
                predicates.add(cb.like(cb.lower(root.get("modesSupported")), "%" + modesSupported.toLowerCase() + "%"));
            if (isActive != null)
                predicates.add(cb.equal(root.get("isActive"), isActive));
            if (bankType != null)
                predicates.add(cb.like(cb.lower(root.get("bankType")), "%" + bankType.toLowerCase() + "%"));
            if (website != null)
                predicates.add(cb.like(cb.lower(root.get("website")), "%" + website.toLowerCase() + "%"));
            if (logo != null)
                predicates.add(cb.like(cb.lower(root.get("logo")), "%" + logo.toLowerCase() + "%"));
            if (parameter != null)
                predicates.add(cb.like(cb.lower(root.get("parameter")), "%" + parameter.toLowerCase() + "%"));
            if (createdBy != null)
                predicates.add(cb.like(cb.lower(root.get("createdBy")), "%" + createdBy.toLowerCase() + "%"));
            if (editedBy != null)
                predicates.add(cb.like(cb.lower(root.get("editedBy")), "%" + editedBy.toLowerCase() + "%"));

            if (createdOn != null) {
                predicates.add(cb.between(root.get("createdOn"),
                        createdOn.atStartOfDay(), createdOn.atTime(23, 59, 59)));
            }

            if (editedOn != null) {
                predicates.add(cb.between(root.get("editedOn"),
                        editedOn.atStartOfDay(), editedOn.atTime(23, 59, 59)));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
