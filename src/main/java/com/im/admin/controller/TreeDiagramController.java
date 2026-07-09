package com.im.admin.controller;

import com.im.admin.request.HierarchyResponse;
import com.im.admin.service.HierarchyAccessService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

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
    @GetMapping(value = "/data", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getHierarchyData() {
        // You can implement this to return raw data if needed
        return ResponseEntity.ok(Map.of(
                "message", "Use /hierarchy/tree for structured tree data"
        ));
    }
}
