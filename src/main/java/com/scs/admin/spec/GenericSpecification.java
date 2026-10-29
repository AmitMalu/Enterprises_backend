package com.scs.admin.spec;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.*;
import jakarta.persistence.metamodel.EntityType;import jakarta.persistence.metamodel.Metamodel;
import jakarta.persistence.metamodel.SingularAttribute;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

public class GenericSpecification<T> implements Specification<T> {

    private final Map<String, Object> filters;

    @PersistenceContext
    private EntityManager entityManager;

    public GenericSpecification(Map<String, Object> filters) {
        this.filters = filters;
    }

    @Override
    public Predicate toPredicate(Root<T> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
        List<Predicate> predicates = new ArrayList<>();

        // Get valid entity fields
        Set<String> entityFields = getEntityFields(root.getModel());

        filters.forEach((field, value) -> {
            if (!entityFields.contains(field) || value == null) {
                return; // skip invalid or null fields
            }

            Path<Object> path = root.get(field);

            if (value instanceof String strVal) {
                predicates.add(cb.like(cb.lower(path.as(String.class)), "%" + strVal.toLowerCase() + "%"));
            } else if (value instanceof LocalDateTime ldt) {
                predicates.add(cb.equal(path.as(LocalDateTime.class), ldt));
            } else if (value instanceof LocalDate ld) {
                predicates.add(cb.equal(path.as(LocalDate.class), ld));
            } else {
                predicates.add(cb.equal(path, value));
            }
        });

        return cb.and(predicates.toArray(new Predicate[0]));
    }

    // Helper: get all field names from entity
    private Set<String> getEntityFields(EntityType<T> entityType) {
        Set<String> fields = new HashSet<>();
        for (SingularAttribute<? super T, ?> attr : entityType.getSingularAttributes()) {
            fields.add(attr.getName());
        }
        return fields;
    }
}


