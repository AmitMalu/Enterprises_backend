package com.im.admin.spec;

import com.im.admin.entities.NetworkBankMaster;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class BankSpecification {

    public static Specification<NetworkBankMaster> getFilter(
            String id,
            String bankCode,
            String bankName,
            String ifscCode,
            String imps,
            String neft,
            String rtgs,
            String activeStatus,
            String createdBy,
            String editedBy,
            Boolean isBlocked,
            LocalDate createdOn,
            LocalDate editedOn) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (id != null)
                predicates.add(cb.like(cb.lower(root.get("id").as(String.class)), "%" + id.toLowerCase() + "%"));
            if (bankCode != null)
                predicates.add(cb.like(cb.lower(root.get("bankCode")), "%" + bankCode.toLowerCase() + "%"));
            if (bankName != null)
                predicates.add(cb.like(cb.lower(root.get("bankName")), "%" + bankName.toLowerCase() + "%"));
            if (ifscCode != null)
                predicates.add(cb.like(cb.lower(root.get("ifscCode")), "%" + ifscCode.toLowerCase() + "%"));
            if (imps != null)
                predicates.add(cb.equal(cb.lower(root.get("imps")), imps.toLowerCase()));
            if (neft != null)
                predicates.add(cb.equal(cb.lower(root.get("neft")), neft.toLowerCase()));
            if (rtgs != null)
                predicates.add(cb.equal(cb.lower(root.get("rtgs")), rtgs.toLowerCase()));
            if (activeStatus != null)
                predicates.add(cb.like(cb.lower(root.get("activeStatus")), "%" + activeStatus.toLowerCase() + "%"));
            if (createdBy != null)
                predicates.add(cb.like(cb.lower(root.get("createdBy")), "%" + createdBy.toLowerCase() + "%"));
            if (editedBy != null)
                predicates.add(cb.like(cb.lower(root.get("editedBy")), "%" + editedBy.toLowerCase() + "%"));
            if (isBlocked != null)
                predicates.add(cb.equal(root.get("isBlocked"), isBlocked));

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
