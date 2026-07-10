package com.im.admin.service;

import com.im.admin.dto.AbstractWalletAmountTable;
import com.im.admin.entities.AccessHierarchyNode;
import com.im.admin.entities.AccessHierarchyPath;
import com.im.admin.entities.AccessHierarchyPathId;
import com.im.admin.repositories.*;
import com.im.admin.request.HierarchyRegistrationRequest;
import com.im.admin.request.HierarchyResponse;
import com.im.admin.request.TreeNode;
import com.im.admin.util.CommonMethods;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author SHUBHAM KHOPADE
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class HierarchyAccessService {

    private final AccessHierarchyNodeRepository nodeRepository;
    private final AccessHierarchyPathRepository pathRepository;
    private final CommonMethods commonMethods;
    private final AccessHierarchyNodeRepository nodeRepo;
    private final AccessHierarchyPathRepository pathRepo;
    private final UserDetailsRepository userDetailsRepository;

    private static class WalletBalanceMaps {
        Map<String, BigDecimal> prepaid = new HashMap<>();
        Map<String, BigDecimal> postpaid = new HashMap<>();
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    private static class UserInfo {
        private String mobileNumber;
        private String otp;
    }

    @Autowired
    private EnterprisesPostpaidAmountRepository enterprisesPostpaidAmountRepository;

    @Autowired
    private EnterprisesPrepaidAmountRepository enterprisesPrepaidAmountRepository;

    @Autowired
    private SuperDistributorPostpaidAmountRepository superDistributorPostpaidAmountRepository;

    @Autowired
    private SuperDistributorPrepaidAmountRepository superDistributorPrepaidAmountRepository;

    @Autowired
    private RetailerPostpaidAmountRepository retailerPostpaidAmountRepository;

    @Autowired
    private RetailerPrepaidAmountRepository retailerPrepaidAmountRepository;

    @Autowired
    private DistributorPostpaidAmountRepository distributorPostpaidAmountRepository;

    @Autowired
    private DistributorPrepaidAmountRepository distributorPrepaidAmountRepository;

    @Autowired
    private PartnerPostpaidAmountRepository partnerPostpaidAmountRepository;

    @Autowired
    private PartnerPrepaidAmountRepository partnerPrepaidAmountRepository;

    @Autowired
    private WhiteLabelPrepaidAmountRepository whiteLabelPrepaidAmountRepository;

    @Autowired
    private WhiteLabelPostpaidAmountRepository whiteLabelPostpaidAmountRepository;

    @Autowired
    private AdminPostpaidAmountRepository adminPostpaidAmountRepository;

    @Autowired
    private AdminPrepaidAmountRepository adminPrepaidAmountRepository;

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


    public HierarchyResponse getHierarchyTree() {
        log.info("=== Starting getHierarchyTree() ===");
        long startTime = System.currentTimeMillis();

        List<AccessHierarchyNode> allNodes = nodeRepository.findAll();
        log.info("Found {} nodes in database", allNodes.size());

        if (allNodes.isEmpty()) {
            throw new RuntimeException("No hierarchy data found");
        }

        WalletBalanceMaps walletMaps = loadAllWalletBalances();

        Map<String, UserInfo> userInfoMap = loadAllUserDetails();

        Map<String, List<AccessHierarchyNode>> childrenByParent =
                allNodes.stream()
                        .filter(node -> node != null
                                && node.getParentHierarchyId() != null
                                && !node.getParentHierarchyId().isBlank()
                                && !node.getParentHierarchyId().equals("-"))
                        .collect(Collectors.groupingBy(AccessHierarchyNode::getParentHierarchyId));

        AccessHierarchyNode rootNode = allNodes.stream()
                .filter(node -> node.getParentHierarchyId() == null
                        || node.getParentHierarchyId().equals("-")
                        || node.getParentHierarchyId().isBlank())
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Root node not found"));

        TreeNode root = buildTreeFast(rootNode, childrenByParent, walletMaps, userInfoMap);

        List<AccessHierarchyPath> paths = pathRepository.findAll();

        Map<String, Integer> depthMap = paths.stream()
                .filter(path -> path != null && path.getId() != null)
                .collect(Collectors.toMap(
                        path -> path.getId().getDescendantId(),
                        AccessHierarchyPath::getDepth,
                        (existing, replacement) -> existing
                ));

        setDepthForNodes(root, depthMap);

        List<TreeNode> allTreeNodes = new ArrayList<>();
        flattenTree(root, allTreeNodes);

        HierarchyResponse response = new HierarchyResponse();
        response.setRoot(root);
        response.setDepthMap(depthMap);
        response.setTotalNodes(allNodes.size());
        response.setAllNodes(allTreeNodes);

        log.info("=== getHierarchyTree() completed in {} ms ===",
                System.currentTimeMillis() - startTime);

        return response;
    }

    private TreeNode buildTreeFast(
            AccessHierarchyNode currentNode,
            Map<String, List<AccessHierarchyNode>> childrenByParent,
            WalletBalanceMaps walletMaps,
            Map<String, UserInfo> userInfoMap
    ) {
        TreeNode treeNode = new TreeNode(
                currentNode.getHierarchyId(),
                currentNode.getHierarchyType(),
                currentNode.getParentHierarchyId(),
                currentNode.getCreatedOn() != null ? currentNode.getCreatedOn().toString() : null
        );

        treeNode.setPrepaidBalance(walletMaps.prepaid.get(currentNode.getHierarchyId()));
        treeNode.setPostpaidBalance(walletMaps.postpaid.get(currentNode.getHierarchyId()));

        UserInfo userInfo = userInfoMap.get(currentNode.getHierarchyId());

        if (userInfo != null) {
            treeNode.setMobileNumber(userInfo.getMobileNumber());
            treeNode.setOpt(userInfo.getOtp());
        }

        List<AccessHierarchyNode> children =
                childrenByParent.getOrDefault(currentNode.getHierarchyId(), Collections.emptyList());

        for (AccessHierarchyNode child : children) {
            treeNode.addChild(buildTreeFast(child, childrenByParent, walletMaps, userInfoMap));
        }

        return treeNode;
    }


    private WalletBalanceMaps loadAllWalletBalances() {
        WalletBalanceMaps maps = new WalletBalanceMaps();

        retailerPrepaidAmountRepository.findAll().forEach(w ->
                maps.prepaid.put(w.getRetailerId().getRetailerId(), w.getAvailableBalance()));

        retailerPostpaidAmountRepository.findAll().forEach(w ->
                maps.postpaid.put(w.getRetailerId().getRetailerId(), w.getAvailableBalance()));

        distributorPrepaidAmountRepository.findAll().forEach(w ->
                maps.prepaid.put(w.getDistributorId().getDistributorId(), w.getAvailableBalance()));

        distributorPostpaidAmountRepository.findAll().forEach(w ->
                maps.postpaid.put(w.getDistributorId().getDistributorId(), w.getAvailableBalance()));

        partnerPrepaidAmountRepository.findAll().forEach(w ->
                maps.prepaid.put(w.getPartnerId().getPartnerId(), w.getAvailableBalance()));

        partnerPostpaidAmountRepository.findAll().forEach(w ->
                maps.postpaid.put(w.getPartnerId().getPartnerId(), w.getAvailableBalance()));

        superDistributorPrepaidAmountRepository.findAll().forEach(w ->
                maps.prepaid.put(w.getSuperDistributorId().getSuperDistributorId(), w.getAvailableBalance()));

        superDistributorPostpaidAmountRepository.findAll().forEach(w ->
                maps.postpaid.put(w.getSuperDistributorId().getSuperDistributorId(), w.getAvailableBalance()));

        whiteLabelPrepaidAmountRepository.findAll().forEach(w ->
                maps.prepaid.put(w.getWhiteLabelId().getWhiteLabelId(), w.getAvailableBalance()));

        whiteLabelPostpaidAmountRepository.findAll().forEach(w ->
                maps.postpaid.put(w.getWhiteLabelId().getWhiteLabelId(), w.getAvailableBalance()));

        adminPrepaidAmountRepository.findAll().forEach(w ->
                maps.prepaid.put(w.getAdminId().getId(), w.getAvailableBalance()));

        adminPostpaidAmountRepository.findAll().forEach(w ->
                maps.postpaid.put(w.getAdminId().getId(), w.getAvailableBalance()));

        enterprisesPrepaidAmountRepository.findAll().forEach(w ->
                maps.prepaid.put(w.getEnterprise().getEnterpriseId(), w.getAvailableBalance()));

        enterprisesPostpaidAmountRepository.findAll().forEach(w ->
                maps.postpaid.put(w.getEnterprise().getEnterpriseId(), w.getAvailableBalance()));

        return maps;
    }

    private void setDepthForNodes(TreeNode node, Map<String, Integer> depthMap) {
        if (node == null) {
            log.warn("setDepthForNodes called with null node, skipping");
            return;
        }

        Integer depth = depthMap.get(node.getId());
        node.setDepth(depth != null ? depth : 0);

        List<TreeNode> children = node.getChildren();
        if (children != null) {
            for (TreeNode child : children) {
                if (child != null) {
                    setDepthForNodes(child, depthMap);
                } else {
                    log.warn("Null child found in children list of node {}", node.getId());
                }
            }
        }
    }

    private void flattenTree(TreeNode node, List<TreeNode> result) {
        if (node == null) {
            log.warn("flattenTree called with null node, skipping");
            return;
        }

        if (result == null) {
            log.error("flattenTree called with null result list");
            return;
        }

        result.add(node);
        List<TreeNode> children = node.getChildren();
        if (children != null) {
            for (TreeNode child : children) {
                if (child != null) {
                    flattenTree(child, result);
                } else {
                    log.warn("Null child found when flattening node {}", node.getId());
                }
            }
        }
    }

    private Map<String, UserInfo> loadAllUserDetails() {

        Map<String, UserInfo> userMap = new HashMap<>();

        userDetailsRepository.findAll()
                .forEach(user -> {

                    userMap.put(
                            user.getHierarchyId(),
                            new UserInfo(
                                    user.getMobileNumber(),
                                    user.getOtp()
                            )
                    );

                });

        return userMap;
    }

}
