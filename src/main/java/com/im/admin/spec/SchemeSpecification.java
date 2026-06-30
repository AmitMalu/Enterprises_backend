package com.im.admin.spec;

import com.im.admin.dto.SchemeFilter;
import com.im.admin.entities.Scheme;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * @author SHUBHAM KHOPADE
 */

@Slf4j
public class SchemeSpecification {

    public static Specification<Scheme> build(

            SchemeFilter filter,

            List<String> subProducts,

            boolean fullAccess

    ) {

        return (
                root,
                query,
                cb
        ) -> {

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

            if (

                    !fullAccess

                            &&

                            !subProducts.isEmpty()

            ) {

                List<Predicate> access =
                        new ArrayList<>();

                for (
                        String sub
                        :
                        subProducts
                ) {

                    access.add(

                            cb.like(

                                    root.get(
                                            "schemeCombinationHashCode"
                                    ),

                                    "__-"
                                            +
                                            sub
                                            +
                                            "-%"
                            )
                    );

                }

                predicates.add(

                        cb.or(

                                access.toArray(
                                        new Predicate[0]
                                )
                        )
                );

            }

            return cb.and(
                    predicates.toArray(
                            new Predicate[0]
                    )
            );

        };

    }

    private static void addLike(

            List<Predicate> p,

            CriteriaBuilder cb,

            Root<Scheme> root,

            String field,

            String value

    ) {

        if (
                value == null
                        ||
                        value.isBlank()
        ) {
            return;
        }

        p.add(

                cb.like(

                        cb.lower(
                                root.get(
                                        field
                                )
                        ),

                        "%"
                                +
                                value
                                        .toLowerCase()
                                +
                                "%"
                )
        );

    }

}