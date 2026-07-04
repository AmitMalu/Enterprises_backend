package com.im.admin.util;

import com.im.admin.dto.GenericPendingRecordDto;
import com.im.admin.repositories.*;
import com.im.admin.request.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

/**
 * @author SHUBHAM KHOPADE
 */
@Component
public class HierarchyAccessValidator {

    @Autowired
    private TokenUtil tokenUtil;

    @Autowired
    private RetailerRepository retailerRepository;

    @Autowired
    private DistributorRepository distributorRepository;

    @Autowired
    private SuperDistributorRepository superDistributorRepository;

    @Autowired
    private PartnerRepository partnerRepository;

    @Autowired
    private WhiteLabelRepository whiteLabelRepository;

    @Autowired
    private EnterprisesRepository enterprisesRepository;

    @Autowired
    private WhiteLabelUserRepository whiteLabelUserRepository;

    @Autowired
    private AdminUserRepository adminUserRepository;

    @Autowired
    private AccessHierarchyPathRepository accessHierarchyPathRepository;

    /**
     * YOUR EXISTING LOGIC (USED HERE)
     */
    public Optional<String> findParentHierarchy(String hierarchyId) {

        String type = extractHierarchyType(hierarchyId);

        Optional<?> entity = switch (type) {
            case "ET" -> enterprisesRepository.findById(hierarchyId);
            case "WL" -> whiteLabelRepository.findById(hierarchyId);
            case "WLAU" -> whiteLabelUserRepository.findById(hierarchyId);
            case "SD" -> superDistributorRepository.findById(hierarchyId);
            case "PT" -> partnerRepository.findById(hierarchyId);
            case "DT" -> distributorRepository.findById(hierarchyId);
            case "RT" -> retailerRepository.findById(hierarchyId);
            case "AU" -> adminUserRepository.findById(hierarchyId);
            default -> Optional.empty();
        };

        return entity.map(this::extractParentAccount);
    }

    private String extractHierarchyType(String id) {
        return id.split("-")[1];
    }

    private String extractParentAccount(Object entity) {
        try {
            return (String) entity.getClass()
                    .getMethod("getParentAccount")
                    .invoke(entity);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * AD and AU can access everything.
     * Other hierarchy types can access only their own subtree.
     */
//    public boolean belongsToHierarchy(
//            String recordHierarchyId,
//            String loginHierarchyId,
//            String loginHierarchyType
//    ) {
//
//        if (recordHierarchyId == null
//                || loginHierarchyId == null
//                || loginHierarchyType == null) {
//            return false;
//        }
//
//        // AD & AU have full access
//        if ("AD".equalsIgnoreCase(loginHierarchyType)
//                || "AU".equalsIgnoreCase(loginHierarchyType)) {
//            return true;
//        }
//
//        // Check hierarchy path
//        return accessHierarchyPathRepository.existsHierarchyPath(
//                loginHierarchyId,
//                recordHierarchyId
//        );
//
//
//    }
}
