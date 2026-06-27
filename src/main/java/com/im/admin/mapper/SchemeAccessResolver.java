package com.im.admin.mapper;

/**
 * @author SHUBHAM KHOPADE
 */
import com.im.admin.dto.SchemeAccessDto;
import com.im.admin.entities.AccessHierarchyNode;
import com.im.admin.entities.SubProducts;
import com.im.admin.repositories.AccessHierarchyNodeRepository;
import com.im.admin.repositories.SubProductsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SchemeAccessResolver {

    private final AccessHierarchyNodeRepository nodeRepo;

    private final SubProductsRepository subRepo;

    public SchemeAccessDto resolve(
            String loginId
    ) {

        log.info(
                "[SCHEME ACCESS] login={}",
                loginId
        );

        AccessHierarchyNode node =

                nodeRepo
                        .findById(
                                loginId
                        )

                        .orElseThrow(() ->

                                new RuntimeException(
                                        "Hierarchy not found"
                                )
                        );

        String type =
                node.getHierarchyType();

        /*
         * AD
         */
        if (
                "AD".equalsIgnoreCase(type)
        ) {

            log.info(
                    "[SCHEME ACCESS] ADMIN → FULL ACCESS"
            );

            return new SchemeAccessDto(
                    true,
                    List.of()
            );
        }

        /*
         * AU UNDER AD
         */
        if (
                type.endsWith("AU")
        ) {

            String parent =
                    node.getParentHierarchyId();

            if (
                    parent != null
                            &&
                            parent.contains("-AD-")
            ) {

                log.info(
                        "[SCHEME ACCESS] AU UNDER AD → FULL ACCESS"
                );

                return new SchemeAccessDto(
                        true,
                        List.of()
                );
            }
        }

        /*
         * WL
         */

        String wlId = null;

        if (
                "WL".equalsIgnoreCase(
                        type
                )
        ) {

            wlId =
                    loginId;
        }

        /*
         * WLAU
         */

        else if (

                type.endsWith(
                        "AU"
                )

        ) {

            String parent =
                    node.getParentHierarchyId();

            if (
                    parent != null
                            &&
                            parent.contains("-WL-")
            ) {

                wlId =
                        parent;
            }
        }

        if (
                wlId != null
        ) {

            List<String> allowed =

                    subRepo

                            .findAllowedWLSubProducts(
                                    wlId
                            )

                            .stream()

                            .map(
                                    SubProducts::getId
                            )

                            .toList();

            log.info(
                    "[SCHEME ACCESS] WL={} allowedSubProducts={}",
                    wlId,
                    allowed
            );

            return new SchemeAccessDto(
                    false,
                    allowed
            );
        }

        log.info(
                "[SCHEME ACCESS] NO ACCESS"
        );

        return new SchemeAccessDto(
                false,
                List.of()
        );

    }

}