package com.im.admin.spec;

import com.im.admin.entities.ImSubMaster;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class DropdownSpecification {

    public static Specification<ImSubMaster> getFilter(
            Integer imMasterId,
            String imMasterName,
            Integer imSubMasterId,
            String imSubMasterName
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (imMasterId != null) {
                predicates.add(cb.equal(root.get("imMasterId"), imMasterId));
            }

            if (imMasterName != null) {
                Join<Object, Object> masterJoin = root.join("imMaster");
                predicates.add(cb.like(cb.lower(masterJoin.get("imMasterName")), "%" + imMasterName.toLowerCase() + "%"));
            }

            if (imSubMasterId != null) {
                predicates.add(cb.equal(root.get("imSubMasterId"), imSubMasterId));
            }

            if (imSubMasterName != null) {
                predicates.add(cb.like(cb.lower(root.get("imSubMasterName")), "%" + imSubMasterName.toLowerCase() + "%"));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
