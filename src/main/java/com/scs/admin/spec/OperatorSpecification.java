package com.scs.admin.spec;

import com.scs.admin.entities.Operators;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class OperatorSpecification {

    public static Specification<Operators> getFilter(List<String> id, String operatorCategory,
            String operatorCode, String operatorName, String operatorAlias, String alias,
            String productCode, String productId,
            String subProductCode, String subProductId,
            Boolean isActive, String createdBy, String editedBy,
            LocalDate createdOn,
            LocalDate editedOn
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (id != null && !id.isEmpty()) {
                List<Predicate> idPreds = new ArrayList<>();
                for (String val : id) {
                    idPreds.add(cb.like(root.get("id"), "%" + val + "%"));
                }
                predicates.add(cb.or(idPreds.toArray(new Predicate[0])));
            }

            if (operatorCategory != null)
                predicates.add(cb.like(cb.lower(root.get("operatorCategory")), "%" + operatorCategory.toLowerCase() + "%"));
            if (operatorCode != null)
                predicates.add(cb.like(cb.lower(root.get("operatorCode")), "%" + operatorCode.toLowerCase() + "%"));
            if (operatorName != null)
                predicates.add(cb.like(cb.lower(root.get("operatorName")), "%" + operatorName.toLowerCase() + "%"));
            if (operatorAlias != null)
                predicates.add(cb.like(cb.lower(root.get("operatorAlias")), "%" + operatorAlias.toLowerCase() + "%"));
            if (alias != null)
                predicates.add(cb.like(cb.lower(root.get("alias")), "%" + alias.toLowerCase() + "%"));
            if (productCode != null)
                predicates.add(cb.like(cb.lower(root.get("productCode")), "%" + productCode.toLowerCase() + "%"));
            if (productId != null)
                predicates.add(cb.like(cb.lower(root.get("productId")), "%" + productId.toLowerCase() + "%"));
            if (subProductCode != null)
                predicates.add(cb.like(cb.lower(root.get("subProductCode")), "%" + subProductCode.toLowerCase() + "%"));
            if (subProductId != null)
                predicates.add(cb.like(cb.lower(root.get("subProductId")), "%" + subProductId.toLowerCase() + "%"));
            if (isActive != null)
                predicates.add(cb.equal(root.get("isActive"), isActive));
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