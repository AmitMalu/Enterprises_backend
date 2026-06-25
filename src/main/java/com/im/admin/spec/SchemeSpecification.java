package com.im.admin.spec;

import com.im.admin.dto.SchemeFilter;
import com.im.admin.entities.Scheme;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author SHUBHAM KHOPADE
 */
public class SchemeSpecification {

    public static Specification<Scheme> build(
            SchemeFilter filter,
            Set<String> accessibleIds,
            boolean fullAccess
    ) {

        return (root, query, cb) -> {

            List<Predicate> predicates =
                    new ArrayList<>();

            addLike(
                    predicates,
                    cb,
                    root,
                    "schemeName",
                    filter.getSchemeName()
            );

            addLike(
                    predicates,
                    cb,
                    root,
                    "schemeCode",
                    filter.getSchemeCode()
            );

            if (!fullAccess) {

                List<Predicate> hierarchy =
                        accessibleIds.stream()

                                .map(id ->

                                        cb.like(
                                                root.get(
                                                        "schemeCombinationHashCode"
                                                ),

                                                "%-" + id + "-%"
                                        )

                                )

                                .toList();

                predicates.add(
                        cb.or(
                                hierarchy.toArray(
                                        new Predicate[0]
                                )
                        ));
            }

            return cb.and(
                    predicates.toArray(
                            new Predicate[0]
                    )
            );
        };
    }

    private static void addLike(
            List<Predicate> predicates,
            CriteriaBuilder cb,
            Root<Scheme> root,
            String field,
            String value
    ) {

        if (value == null ||
                value.isBlank()) {
            return;
        }

        predicates.add(

                cb.like(

                        cb.lower(
                                root.get(field)
                        ),

                        "%"
                                + value.toLowerCase()
                                + "%"
                )
        );
    }
}
