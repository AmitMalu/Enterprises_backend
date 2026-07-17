package com.im.admin.service;

import com.im.admin.dto.AbstractWalletAmountTable;
import com.im.admin.dto.SchemeAllocationDto;
import com.im.admin.entities.*;
import com.im.admin.entities.product.Products;
import com.im.admin.repositories.*;
import com.im.admin.request.*;
import com.im.admin.response.SchemeAllocationDetails;
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
    private final SchemeAllocationRepository schemeAllocationRepository;
    private final ProductsRepository productsRepository;
    private final SubProductsRepository subProductsRepository;
    private final SchemeRepository schemeRepository;


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

//    // Update the main method
//    public HierarchyResponse getHierarchyTree() {
//        log.info("=== Starting getHierarchyTree() ===");
//        long startTime = System.currentTimeMillis();
//
//        List<AccessHierarchyNode> allNodes = nodeRepository.findAll();
//        log.info("Found {} nodes in database", allNodes.size());
//
//        if (allNodes.isEmpty()) {
//            throw new RuntimeException("No hierarchy data found");
//        }
//
//        WalletBalanceMaps walletMaps = loadAllWalletBalances();
//        Map<String, UserInfo> userInfoMap = loadAllUserDetails();
//
//        // ===== LOAD SCHEME ALLOCATIONS =====
//        Map<String, List<SchemeAllocation>> schemeAllocationsMap = loadAllSchemeAllocations();
//
//        Map<String, List<AccessHierarchyNode>> childrenByParent =
//                allNodes.stream()
//                        .filter(node -> node != null
//                                && node.getParentHierarchyId() != null
//                                && !node.getParentHierarchyId().isBlank()
//                                && !node.getParentHierarchyId().equals("-"))
//                        .collect(Collectors.groupingBy(AccessHierarchyNode::getParentHierarchyId));
//
//        AccessHierarchyNode rootNode = allNodes.stream()
//                .filter(node -> node.getParentHierarchyId() == null
//                        || node.getParentHierarchyId().equals("-")
//                        || node.getParentHierarchyId().isBlank())
//                .findFirst()
//                .orElseThrow(() -> new RuntimeException("Root node not found"));
//
//        TreeNode root = buildTreeFast(rootNode, childrenByParent, walletMaps, userInfoMap, schemeAllocationsMap);
//
//        List<AccessHierarchyPath> paths = pathRepository.findAll();
//
//        Map<String, Integer> depthMap = paths.stream()
//                .filter(path -> path != null && path.getId() != null)
//                .collect(Collectors.toMap(
//                        path -> path.getId().getDescendantId(),
//                        AccessHierarchyPath::getDepth,
//                        (existing, replacement) -> existing
//                ));
//
//        setDepthForNodes(root, depthMap);
//
//        List<TreeNode> allTreeNodes = new ArrayList<>();
//        flattenTree(root, allTreeNodes);
//
//        HierarchyResponse response = new HierarchyResponse();
//        response.setRoot(root);
//        response.setDepthMap(depthMap);
//        response.setTotalNodes(allNodes.size());
//        response.setAllNodes(allTreeNodes);
//
//        log.info("=== getHierarchyTree() completed in {} ms ===",
//                System.currentTimeMillis() - startTime);
//
//        return response;
//    }
//
//    // ===== LOAD SCHEME ALLOCATIONS =====
//    private Map<String, List<SchemeAllocation>> loadAllSchemeAllocations() {
//        log.info("Loading scheme allocations...");
//        long startTime = System.currentTimeMillis();
//
//        List<SchemeAllocation> allAllocations = schemeAllocationRepository.findAll();
//
//        Map<String, List<SchemeAllocation>> schemeMap = allAllocations.stream()
//                .filter(sa -> sa != null && sa.getHierarchyId() != null)
//                .collect(Collectors.groupingBy(SchemeAllocation::getHierarchyId));
//
//        log.info("Loaded {} scheme allocations for {} hierarchies in {} ms",
//                allAllocations.size(), schemeMap.size(), System.currentTimeMillis() - startTime);
//
//        return schemeMap;
//    }
//
//    // ===== UPDATED BUILD TREE METHOD =====
//    private TreeNode buildTreeFast(
//            AccessHierarchyNode currentNode,
//            Map<String, List<AccessHierarchyNode>> childrenByParent,
//            WalletBalanceMaps walletMaps,
//            Map<String, UserInfo> userInfoMap,
//            Map<String, List<SchemeAllocation>> schemeAllocationsMap  // NEW PARAMETER
//    ) {
//        TreeNode treeNode = new TreeNode(
//                currentNode.getHierarchyId(),
//                currentNode.getHierarchyType(),
//                currentNode.getParentHierarchyId(),
//                currentNode.getCreatedOn() != null ? currentNode.getCreatedOn().toString() : null
//        );
//
//        treeNode.setPrepaidBalance(walletMaps.prepaid.get(currentNode.getHierarchyId()));
//        treeNode.setPostpaidBalance(walletMaps.postpaid.get(currentNode.getHierarchyId()));
//
//        UserInfo userInfo = userInfoMap.get(currentNode.getHierarchyId());
//        if (userInfo != null) {
//            treeNode.setMobileNumber(userInfo.getMobileNumber());
//            treeNode.setOtp(userInfo.getOtp());
//        }
//
//        // ===== ADD SCHEME ALLOCATIONS =====
//        List<SchemeAllocation> schemes = schemeAllocationsMap.get(currentNode.getHierarchyId());
//        if (schemes != null && !schemes.isEmpty()) {
//            treeNode.setSchemeAllocations(schemes);
//            treeNode.setSchemesByProduct(groupSchemesByProduct(schemes));
//            log.debug("Added {} scheme allocations to node {}", schemes.size(), currentNode.getHierarchyId());
//        }
//
//        List<AccessHierarchyNode> children =
//                childrenByParent.getOrDefault(currentNode.getHierarchyId(), Collections.emptyList());
//
//        for (AccessHierarchyNode child : children) {
//            treeNode.addChild(buildTreeFast(child, childrenByParent, walletMaps, userInfoMap, schemeAllocationsMap));
//        }
//
//        return treeNode;
//    }

//    // ===== GROUP SCHEMES BY PRODUCT =====
//    private Map<String, List<SchemeAllocation>> groupSchemesByProduct(List<SchemeAllocation> schemes) {
//        if (schemes == null || schemes.isEmpty()) {
//            return Collections.emptyMap();
//        }
//
//        return schemes.stream()
//                .collect(Collectors.groupingBy(SchemeAllocation::getProductCode));
//    }

//    // ===== GET ONLY RETAILER SCHEMES (Optional) =====
//    private List<SchemeAllocation> getRetailerSchemes(String hierarchyId) {
//        List<SchemeAllocation> allSchemes = schemeAllocationRepository.findByHierarchyId(hierarchyId);
//        return allSchemes.stream()
//                .filter(sa -> "RT".equals(commonMethods.extractHierarchyType(sa.getHierarchyId())))
//                .collect(Collectors.toList());
//    }

    // ===== GET SCHEMES WITH PRODUCT DETAILS (Enhanced) =====
    private SchemeAllocationDetails getSchemeAllocationDetails(String hierarchyId) {
        List<SchemeAllocation> schemes = schemeAllocationRepository.findByHierarchyId(hierarchyId);

        // Group by product
        Map<String, List<SchemeAllocation>> byProduct = schemes.stream()
                .collect(Collectors.groupingBy(SchemeAllocation::getProductCode));

        SchemeAllocationDetails details = new SchemeAllocationDetails();
        // Build the nested structure...

        return details;
    }

    // ===== OPTIONAL: GET ONLY APPLICABLE SCHEMES FOR RETAILER =====
//    private List<SchemeAllocation> getApplicableSchemes(String hierarchyId) {
//        // You can add logic here to filter schemes based on hierarchy type
//        // For example, only return schemes where hierarchy type is "RT" (retailer)
//        return schemeAllocationRepository.findByHierarchyIdAndHierarchyType(hierarchyId, "RT");
//    }

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

//    private void setDepthForNodes(TreeNode node, Map<String, Integer> depthMap) {
//        if (node == null) {
//            log.warn("setDepthForNodes called with null node, skipping");
//            return;
//        }
//
//        Integer depth = depthMap.get(node.getId());
//        node.setDepth(depth != null ? depth : 0);
//
//        List<TreeNode> children = node.getChildren();
//        if (children != null) {
//            for (TreeNode child : children) {
//                if (child != null) {
//                    setDepthForNodes(child, depthMap);
//                } else {
//                    log.warn("Null child found in children list of node {}", node.getId());
//                }
//            }
//        }
//    }
//
//    private void flattenTree(TreeNode node, List<TreeNode> result) {
//        if (node == null) {
//            log.warn("flattenTree called with null node, skipping");
//            return;
//        }
//
//        if (result == null) {
//            log.error("flattenTree called with null result list");
//            return;
//        }
//
//        result.add(node);
//        List<TreeNode> children = node.getChildren();
//        if (children != null) {
//            for (TreeNode child : children) {
//                if (child != null) {
//                    flattenTree(child, result);
//                } else {
//                    log.warn("Null child found when flattening node {}", node.getId());
//                }
//            }
//        }
//    }

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


    // ===== MAIN METHOD =====
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

        // ===== LOAD SCHEME GROUPS WITH NAMES =====
        Map<String, List<ProductSchemeGroup>> schemeGroupsMap = loadAllSchemesGroupedByProduct();

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

        TreeNode root = buildTreeFast(rootNode, childrenByParent, walletMaps, userInfoMap, schemeGroupsMap);

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

    // ===== LOAD SCHEME GROUPS WITH PRODUCT NAMES =====
    private Map<String, List<ProductSchemeGroup>> loadAllSchemesGroupedByProduct() {
        log.info("Loading scheme allocations grouped by product with names...");
        long startTime = System.currentTimeMillis();

        // 1. Load all scheme allocations
        List<SchemeAllocation> allocations = schemeAllocationRepository.findAll();

        if (allocations.isEmpty()) {
            log.info("No scheme allocations found");
            return Collections.emptyMap();
        }

        // 2. Load name maps
        Map<String, String> productNameMap = loadProductNameMap();
        Map<String, String> schemeNameMap = loadSchemeNameMap();
        Map<String, String> subProductNameMap = loadSubProductNameMap();

        // 3. Group by hierarchyId
        Map<String, List<SchemeAllocation>> byHierarchy = allocations.stream()
                .filter(sa -> sa != null && sa.getHierarchyId() != null)
                .collect(Collectors.groupingBy(SchemeAllocation::getHierarchyId));

        Map<String, List<ProductSchemeGroup>> result = new HashMap<>();

        for (Map.Entry<String, List<SchemeAllocation>> entry : byHierarchy.entrySet()) {
            String hierarchyId = entry.getKey();
            List<SchemeAllocation> hierarchyAllocations = entry.getValue();

            // Group by product code within this hierarchy
            Map<String, List<SchemeAllocation>> byProduct = hierarchyAllocations.stream()
                    .collect(Collectors.groupingBy(SchemeAllocation::getProductCode));

            List<ProductSchemeGroup> productGroups = new ArrayList<>();

            for (Map.Entry<String, List<SchemeAllocation>> productEntry : byProduct.entrySet()) {
                String productCode = productEntry.getKey();
                List<SchemeAllocation> productSchemes = productEntry.getValue();

                // Group by scheme code within this product
                Map<String, List<SchemeAllocation>> byScheme = productSchemes.stream()
                        .collect(Collectors.groupingBy(SchemeAllocation::getSchemeCode));

                List<SchemeGroup> schemeGroups = new ArrayList<>();

                for (Map.Entry<String, List<SchemeAllocation>> schemeEntry : byScheme.entrySet()) {
                    String schemeCode = schemeEntry.getKey();
                    List<SchemeAllocation> schemeSubProducts = schemeEntry.getValue();

                    // Convert to SubProductInfo
                    List<SubProductInfo> subProducts = schemeSubProducts.stream()
                            .map(sa -> new SubProductInfo(
                                    sa.getSubProductCode(),
                                    subProductNameMap.get(sa.getSubProductCode())
                            ))
                            .collect(Collectors.toList());

                    schemeGroups.add(new SchemeGroup(
                            schemeCode,
                            schemeNameMap.get(schemeCode),
                            subProducts
                    ));
                }

                productGroups.add(new ProductSchemeGroup(
                        productCode,
                        productNameMap.get(productCode),
                        schemeGroups
                ));
            }

            result.put(hierarchyId, productGroups);
        }

        log.info("Loaded scheme groups for {} hierarchies in {} ms",
                result.size(), System.currentTimeMillis() - startTime);

        return result;
    }

    // ===== LOAD PRODUCT NAMES =====
    private Map<String, String> loadProductNameMap() {
        List<Products> products = productsRepository.findAll();
        Map<String, String> map = new HashMap<>();
        for (Products p : products) {
            if (p != null && p.getProductCode() != null) {
                map.put(p.getProductCode(),
                        p.getName() != null ? p.getName() : p.getProductCode());
            }
        }
        return map;
    }

    // ===== LOAD SCHEME NAMES =====
    private Map<String, String> loadSchemeNameMap() {
        List<Scheme> schemes = schemeRepository.findAll();
        Map<String, String> map = new HashMap<>();
        for (Scheme s : schemes) {
            if (s != null && s.getSchemeCode() != null) {
                map.put(s.getSchemeCode(),
                        s.getSchemeName() != null ? s.getSchemeName() : s.getSchemeCode());
            }
        }
        return map;
    }

    // ===== LOAD SUBPRODUCT NAMES =====
    private Map<String, String> loadSubProductNameMap() {
        List<SubProducts> subProducts = subProductsRepository.findAll();
        Map<String, String> map = new HashMap<>();
        for (SubProducts sp : subProducts) {
            if (sp != null && sp.getSubProductCode() != null) {
                map.put(sp.getSubProductCode(),
                        sp.getSubProductTitle() != null ? sp.getSubProductTitle(): sp.getSubProductCode());
            }
        }
        return map;
    }

    // ===== UPDATED BUILD TREE METHOD =====
    private TreeNode buildTreeFast(
            AccessHierarchyNode currentNode,
            Map<String, List<AccessHierarchyNode>> childrenByParent,
            WalletBalanceMaps walletMaps,
            Map<String, UserInfo> userInfoMap,
            Map<String, List<ProductSchemeGroup>> schemeGroupsMap
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
            treeNode.setOtp(userInfo.getOtp());
        }

        // ===== ADD PRODUCT GROUPS WITH SCHEMES =====
        List<ProductSchemeGroup> productGroups = schemeGroupsMap.get(currentNode.getHierarchyId());
        if (productGroups != null && !productGroups.isEmpty()) {
            treeNode.setProducts(productGroups);
            treeNode.setHasSchemes(true);
            log.debug("Added {} product groups to node {}", productGroups.size(), currentNode.getHierarchyId());
        }

        List<AccessHierarchyNode> children =
                childrenByParent.getOrDefault(currentNode.getHierarchyId(), Collections.emptyList());

        for (AccessHierarchyNode child : children) {
            treeNode.addChild(buildTreeFast(child, childrenByParent, walletMaps, userInfoMap, schemeGroupsMap));
        }

        return treeNode;
    }

    // ===== SET DEPTH FOR NODES =====
    private void setDepthForNodes(TreeNode node, Map<String, Integer> depthMap) {
        if (node == null) {
            return;
        }

        Integer depth = depthMap.get(node.getId());
        node.setDepth(depth != null ? depth : 0);

        List<TreeNode> children = node.getChildren();
        if (children != null) {
            for (TreeNode child : children) {
                if (child != null) {
                    setDepthForNodes(child, depthMap);
                }
            }
        }
    }

    // ===== FLATTEN TREE =====
    private void flattenTree(TreeNode node, List<TreeNode> result) {
        if (node == null || result == null) {
            return;
        }

        result.add(node);
        List<TreeNode> children = node.getChildren();
        if (children != null) {
            for (TreeNode child : children) {
                if (child != null) {
                    flattenTree(child, result);
                }
            }
        }
    }

    // ===== OPTIONAL: GET ONLY RETAILER SCHEMES =====
    private List<SchemeAllocation> getRetailerSchemes(String hierarchyId) {
        List<SchemeAllocation> allSchemes = schemeAllocationRepository.findByHierarchyId(hierarchyId);
        return allSchemes.stream()
                .filter(sa -> "RT".equals(commonMethods.extractHierarchyType(sa.getHierarchyId())))
                .collect(Collectors.toList());
    }


}
