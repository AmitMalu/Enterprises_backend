package com.im.admin.spec;

import com.im.admin.entities.product.Products;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;


public class ProductsSpecification {

    public static Specification<Products> getFilter(List<Short> id,String name, String productCode, String productCodeForScheme, String productGroup, Boolean isShow, Boolean hasCharges, Boolean isCommissions, Boolean isInventory, Boolean isSettlement, Boolean isVirtual, Boolean isVisible, Boolean reverseTx, Boolean clusterMode, Boolean blocked, Boolean active) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (id != null && !id.isEmpty()) {
                List<Predicate> idPredicates = new ArrayList<>();
                for (Short shortId : id) {
                    idPredicates.add(cb.like(cb.function("str", String.class, root.get("id")), "%" + shortId + "%"));
                }
                predicates.add(cb.or(idPredicates.toArray(new Predicate[0])));
            }


            if (name != null) predicates.add(cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%"));
            if (productCode != null) predicates.add(cb.like(cb.lower(root.get("productCode")), "%" + productCode.toLowerCase() + "%"));
            if (productCodeForScheme != null) predicates.add(cb.like(cb.lower(root.get("productCodeForScheme")), "%" + productCodeForScheme.toLowerCase() + "%"));
            if (productGroup != null) predicates.add(cb.like(cb.lower(root.get("productGroup")), "%" + productGroup.toLowerCase() + "%"));
            if (isShow != null) predicates.add(cb.equal(root.get("inventory").get("isShow"), isShow)); // This is correct
            if (hasCharges != null) predicates.add(cb.equal(root.get("hasCharges"), hasCharges));
            if (isCommissions != null) predicates.add(cb.equal(root.get("isCommissions"), isCommissions));
            if (isInventory != null) predicates.add(cb.equal(root.get("inventory").get("isInventory"), isInventory));
            if (isSettlement != null) predicates.add(cb.equal(root.get("isSettlement"), isSettlement));
            if (isVirtual != null) predicates.add(cb.equal(root.get("inventory").get("isVirtual"), isVirtual)); //  Corrected
            if (isVisible != null) predicates.add(cb.equal(root.get("isVisible"), isVisible));
            if (reverseTx != null) predicates.add(cb.equal(root.get("reverseTx"), reverseTx));
            if (clusterMode != null) predicates.add(cb.equal(root.get("clusterMode"), clusterMode));
            if (blocked != null) predicates.add(cb.equal(root.get("blocked"), blocked));
            if (active != null) predicates.add(cb.equal(root.get("active"), active));

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
    }

