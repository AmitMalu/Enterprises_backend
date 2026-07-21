package com.im.admin.controller;

import com.im.admin.request.HierarchyResponse;
import com.im.admin.response.CommonResponse;
import com.im.admin.service.HierarchyAccessService;
import com.im.admin.service.impl.HierarchyDataService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author SHUBHAM KHOPADE
 */

@Slf4j
@Controller
@RequestMapping("/api/hierarchies")
public class TreeDiagramController {

    @Autowired
    private HierarchyAccessService hierarchyAccessService;

    @Autowired
    private HierarchyDataService hierarchyDataService;


    /**
     * Endpoint 1: Returns JSON data for the hierarchy tree
     * URL: https://api.nfttech.in/enterprises/hierarchies/tree
     */
    @GetMapping(value = "/tree-diagram", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<HierarchyResponse> getHierarchyTree() {
        try {
            HierarchyResponse response = hierarchyAccessService.getHierarchyTree();
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Endpoint 2: Returns HTML view of the hierarchy tree
     * URL: https://api.nfttech.in/enterprises/hierarchies/view
     */
    @GetMapping("/view")
    public String getHierarchyView(Map<String, Object> model) {
        try {
            HierarchyResponse response = hierarchyAccessService.getHierarchyTree();
            model.put("root", response.getRoot());
            model.put("totalNodes", response.getTotalNodes());
            model.put("depthMap", response.getDepthMap());
            return "hierarchy"; // This maps to hierarchy.html in templates folder
        } catch (Exception e) {
            model.put("error", e.getMessage());
            return "error";
        }
    }

    /**
     * Endpoint 3: Simple JSON with just nodes and paths (alternative)
     * URL: https://api.nfttech.in/enterprises/hierarchy/data
     */
    @GetMapping(
            value = "/data",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @ResponseBody
    public ResponseEntity<Map<String, Object>>
    getHierarchyData(

            @RequestParam(name = "hierarchyId", required = false) String hierarchyId,

            @RequestParam(name = "mobileNumber", required = false) String mobileNumber
    ) {

        log.info(
                "Hierarchy complete data API called. hierarchyId: {}, mobileNumberProvided: {}",
                hierarchyId,
                mobileNumber != null &&
                        !mobileNumber.isBlank()
        );

        Map<String, Object> hierarchyData =
                hierarchyDataService
                        .getCompleteHierarchyData(
                                hierarchyId,
                                mobileNumber
                        );

        Map<String, Object> response =
                new LinkedHashMap<>();

        response.put("status", "SUCCESS");

        response.put(
                "message",
                "Hierarchy details fetched successfully"
        );

        response.put("data", hierarchyData);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/grouped-hierarchy")
    public ResponseEntity<
            CommonResponse<Map<String, List<String>>>> getGroupedHierarchyUnderLogin() {

        log.info( "[GROUPED HIERARCHY CONTROLLER] Request received" );

        Map<String, List<String>> result = hierarchyDataService.getGroupedHierarchyUnderLogin();

        String message = result.isEmpty()
                ? "No hierarchy found under the logged-in user."
                : "Hierarchy group fetched successfully.";

        return ResponseEntity.ok(
                CommonResponse.success(
                        result,
                        message
                )
        );
    }
}
