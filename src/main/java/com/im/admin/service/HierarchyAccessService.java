package com.im.admin.service;

import com.im.admin.entities.AccessHierarchyNode;
import com.im.admin.entities.AccessHierarchyPath;
import com.im.admin.entities.AccessHierarchyPathId;
import com.im.admin.repositories.AccessHierarchyNodeRepository;
import com.im.admin.repositories.AccessHierarchyPathRepository;
import com.im.admin.request.HierarchyRegistrationRequest;
import com.im.admin.util.CommonMethods;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * @author SHUBHAM KHOPADE
 */
@Service
@RequiredArgsConstructor
public class HierarchyAccessService {

    private final AccessHierarchyNodeRepository nodeRepository;
    private final AccessHierarchyPathRepository pathRepository;
    private final CommonMethods commonMethods;
    private final AccessHierarchyNodeRepository nodeRepo;
    private final AccessHierarchyPathRepository pathRepo;

    @Transactional
    public void registerHierarchyNode(
            HierarchyRegistrationRequest request) {

        AccessHierarchyNode node = new AccessHierarchyNode();

        node.setHierarchyId(request.getHierarchyId());
        node.setHierarchyType(request.getHierarchyType());
        node.setParentHierarchyId(request.getParentHierarchyId());
        node.setCreatedOn(LocalDateTime.now());

        nodeRepository.save(node);

        buildClosurePath(
                request.getHierarchyId(),
                request.getParentHierarchyId());
    }

    @Transactional
    public void buildClosurePath(
            String hierarchyId,
            String parentHierarchyId) {

        List<AccessHierarchyPath> records = new ArrayList<>();

        // self relation

        records.add(
                createPath(
                        hierarchyId,
                        hierarchyId,
                        0
                )
        );

        if (parentHierarchyId != null) {

            List<AccessHierarchyPath> parentPaths =
                    pathRepository
                            .findByIdDescendantId(parentHierarchyId);

            for (AccessHierarchyPath parentPath : parentPaths) {

                records.add(
                        createPath(
                                parentPath.getId().getAncestorId(),
                                hierarchyId,
                                parentPath.getDepth() + 1
                        )
                );
            }
        }

        pathRepository.saveAll(records);
    }

    private AccessHierarchyPath createPath(
            String ancestor,
            String descendant,
            Integer depth) {

        AccessHierarchyPath path =
                new AccessHierarchyPath();

        path.setId(
                new AccessHierarchyPathId(
                        ancestor,
                        descendant
                )
        );

        path.setDepth(depth);

        return path;
    }

    public boolean hasAccessToHierarchy(
            String loginHierarchyId,
            String targetHierarchyId
    ) {

        if (loginHierarchyId == null
                || targetHierarchyId == null) {
            return false;
        }

        String loginType =
                commonMethods.extractHierarchyType(
                        loginHierarchyId
                );

        // Admin → full access
        if ("AD".equalsIgnoreCase(
                loginType
        )) {
            return true;
        }

        return pathRepository.existsById(
                new AccessHierarchyPathId(
                        loginHierarchyId,
                        targetHierarchyId
                )
        );
    }

    public Set<String> resolveAccessibleHierarchyIds(
            String hierarchyId
    ) {

        Set<String> result =
                new HashSet<>();

        AccessHierarchyNode node =
                nodeRepo
                        .findById(
                                hierarchyId
                        )
                        .orElseThrow(() ->

                                new RuntimeException(
                                        "Hierarchy not found: "
                                                + hierarchyId
                                )
                        );

        String type =
                node.getHierarchyType();

        /*
         * AD → Full Access
         */
        if ("AD".equalsIgnoreCase(type)) {

            return Collections.emptySet();
        }

        /*
         * Always include self
         */
        result.add(
                hierarchyId
        );

        /*
         * AU
         */
        if (isOperationalUser(type)) {

            String parent =
                    node.getParentHierarchyId();

            if (
                    parent != null
                            &&
                            !"-".equals(parent)
            ) {

                result.add(parent);

                List<String> descendants =
                        pathRepo
                                .findDescendantsByAncestor(
                                        parent
                                );

                result.addAll(
                        descendants
                );
            }

        }

        /*
         * Owner/Admin
         */
        else {

            result.addAll(

                    pathRepo
                            .findDescendantsByAncestor(
                                    hierarchyId
                            )
            );
        }

        return result;
    }

    private boolean isOperationalUser(
            String hierarchyType
    ) {
        return hierarchyType.endsWith("AU");
    }

}
