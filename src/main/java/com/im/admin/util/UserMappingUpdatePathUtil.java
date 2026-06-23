package com.im.admin.util;

import com.im.admin.entities.AccessHierarchyPath;
import com.im.admin.entities.AccessHierarchyPathId;
import com.im.admin.repositories.AccessHierarchyNodeRepository;
import com.im.admin.repositories.AccessHierarchyPathRepository;
import com.im.admin.repositories.UserDetailsRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * @author SHUBHAM KHOPADE
 */
@Component
@Slf4j
public class UserMappingUpdatePathUtil {

    @Autowired
    private UserDetailsRepository userDetailsRepository;

    @Autowired
    private AccessHierarchyNodeRepository accessHierarchyNodeRepository;

    @Autowired
    private AccessHierarchyPathRepository accessHierarchyPathRepository;

    @Transactional
    public void updateHierarchyMapping(
            String hierarchyId,
            String newParentId
    ) {

        log.info(
                "Updating hierarchy mapping | hierarchyId={} | newParent={}",
                hierarchyId,
                newParentId
        );

        /*
         * USER DETAILS
         */

        userDetailsRepository
                .findByHierarchyId(
                        hierarchyId
                )
                .ifPresent(user -> {

                    user.setParentAccount(
                            newParentId
                    );

                    userDetailsRepository.save(
                            user
                    );

                    log.info(
                            "UserDetails updated"
                    );
                });

        /*
         * ACCESS_HIERARCHY_NODE
         */

        accessHierarchyNodeRepository
                .findById(
                        hierarchyId
                )
                .ifPresent(node -> {

                    node.setParentHierarchyId(
                            newParentId
                    );

                    accessHierarchyNodeRepository
                            .save(
                                    node
                            );

                    log.info(
                            "AccessHierarchyNode updated"
                    );
                });

        /*
         * ACCESS_HIERARCHY_PATH
         */

        rebuildHierarchyPath(
                hierarchyId,
                newParentId
        );

        log.info(
                "Hierarchy path rebuilt"
        );
    }

    @Transactional
    public void rebuildHierarchyPath(
            String hierarchyId,
            String newParentId
    ) {

        accessHierarchyPathRepository
                .deleteByDescendantExcludingSelf(
                        hierarchyId
                );

        List<AccessHierarchyPath> parentPaths =
                accessHierarchyPathRepository
                        .findAllByDescendant(
                                newParentId
                        );

        List<AccessHierarchyPath> rows =
                new ArrayList<>();

        rows.add(
                buildPath(
                        hierarchyId,
                        hierarchyId,
                        0
                )
        );

        for (AccessHierarchyPath p : parentPaths) {

            rows.add(
                    buildPath(
                            p.getId()
                                    .getAncestorId(),

                            hierarchyId,

                            p.getDepth()
                                    + 1
                    )
            );
        }

        accessHierarchyPathRepository
                .saveAll(
                        rows
                );
    }

    private AccessHierarchyPath buildPath(
            String ancestor,
            String descendant,
            int depth
    ) {

        AccessHierarchyPath path =
                new AccessHierarchyPath();

        path.setId(
                new AccessHierarchyPathId(
                        ancestor,
                        descendant
                )
        );

        path.setDepth(
                depth
        );

        return path;
    }
}
