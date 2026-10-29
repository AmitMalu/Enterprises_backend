package com.scs.admin.spec;

import com.scs.admin.entities.ScsSubMaster;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class DropdownSpecification {

    public static Specification<ScsSubMaster> getFilter(
            Integer scsMasterId,
            String scsMasterName,
            Integer scsSubMasterId,
            String scsSubMasterName
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (scsMasterId != null) {
                predicates.add(cb.equal(root.get("scsMasterId"), scsMasterId));
            }

            if (scsMasterName != null) {
                Join<Object, Object> masterJoin = root.join("scsMaster");
                predicates.add(cb.like(cb.lower(masterJoin.get("scsMasterName")), "%" + scsMasterName.toLowerCase() + "%"));
            }

            if (scsSubMasterId != null) {
                predicates.add(cb.equal(root.get("scsSubMasterId"), scsSubMasterId));
            }

            if (scsSubMasterName != null) {
                predicates.add(cb.like(cb.lower(root.get("scsSubMasterName")), "%" + scsSubMasterName.toLowerCase() + "%"));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
