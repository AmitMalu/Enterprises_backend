package com.im.admin.spec;

import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GenericSpecification<T> implements Specification<T> {

    private final Map<String, Object> filters;

    public GenericSpecification(Map<String, Object> filters) {
        this.filters = filters;
    }

    @Override
    public Predicate toPredicate(Root<T> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
        List<Predicate> predicates = new ArrayList<>();

        filters.forEach((field, value) -> {
            if (value != null) {
                Path<Object> path = root.get(field);

                if (value instanceof String strVal) {
                    // Use LIKE for Strings
                    predicates.add(cb.like(cb.lower(path.as(String.class)), "%" + strVal.toLowerCase() + "%"));
                } else if (value instanceof LocalDateTime ldt) {
                    predicates.add(cb.equal(path.as(LocalDateTime.class), ldt));
                } else if (value instanceof LocalDate ld) {
                    predicates.add(cb.equal(path.as(LocalDate.class), ld));
                } else {
                    // Default: exact match
                    predicates.add(cb.equal(path, value));
                }
            }
        });

        return cb.and(predicates.toArray(new Predicate[0]));
    }
}

