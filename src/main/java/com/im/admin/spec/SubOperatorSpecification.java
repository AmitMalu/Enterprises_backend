package com.im.admin.spec;

import com.im.admin.entities.SubOperator;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class SubOperatorSpecification {

    public static Specification<SubOperator> getFilter(
            List<String> id,
            String subOperatorName,
            String operatorCode,
            String operatorAlias,
            String category,
            String operatorName,
            Boolean isBlocked,
            String createdBy,
            String editedBy,
            LocalDate createdOn,
            LocalDate editedOn
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (id != null && !id.isEmpty()) {
                List<Predicate> idPreds = new ArrayList<>();
                for (String i : id) {
                    idPreds.add(cb.like(cb.lower(root.get("id")), "%" + i.toLowerCase() + "%"));
                }
                predicates.add(cb.or(idPreds.toArray(new Predicate[0])));
            }

            if (subOperatorName != null)
                predicates.add(cb.like(cb.lower(root.get("subOperatorName")), "%" + subOperatorName.toLowerCase() + "%"));

            if (operatorCode != null)
                predicates.add(cb.like(cb.lower(root.get("operatorCode")), "%" + operatorCode.toLowerCase() + "%"));

            if (operatorAlias != null)
                predicates.add(cb.like(cb.lower(root.get("operatorAlias")), "%" + operatorAlias.toLowerCase() + "%"));

            if (operatorName != null)
                predicates.add(cb.like(cb.lower(root.get("operatorName")), "%" + operatorName.toLowerCase() + "%"));


            if (category != null)
                predicates.add(cb.like(cb.lower(root.get("category")), "%" + category.toLowerCase() + "%"));

            if (isBlocked != null)
                predicates.add(cb.equal(root.get("isBlocked"), isBlocked));

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
