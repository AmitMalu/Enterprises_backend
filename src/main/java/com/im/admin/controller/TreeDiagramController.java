package com.im.admin.controller;

import com.im.admin.request.HierarchyResponse;
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

    //Response of above API.

//    {
//        "status": "SUCCESS",
//            "message": "Hierarchy details fetched successfully",
//            "data": {
//        "searchInformation": {
//            "searchType": "HIERARCHY_ID",
//                    "searchValue": "NFT-RT-001",
//                    "resolvedHierarchyId": "NFT-RT-001",
//                    "hierarchyCode": "RT"
//        },
//        "userInformation": {
//            "hierarchyId": "NFT-RT-001",
//                    "hierarchyCode": "RT",
//                    "mobileNumber": "9876543210",
//                    "emailId": "retailer@example.com",
//                    "loginUserName": "retailer001",
//                    "userStatus": "ACTIVE",
//                    "parentAccount": "NFT-DT-001"
//        },
//        "basicInformation": {
//            "hierarchyType": "RT",
//                    "hierarchyId": "NFT-RT-001",
//                    "parentAccount": "NFT-DT-001",
//                    "title": "Mr",
//                    "name": "Retailer Name",
//                    "mobileNumber": "9876543210",
//                    "emailId": "retailer@example.com",
//                    "status": "APPROVED",
//                    "mainAddress": {},
//            "outletAddress": {},
//            "verification": {
//                "emailVerified": true,
//                        "panVerified": true,
//                        "aadhaarVerified": true,
//                        "aadhaarPanVerified": true,
//                        "gstVerified": false
//            }
//        },
//        "walletInformation": {
//            "prepaid": {
//                "available": true,
//                        "id": 1,
//                        "retailer_id": "NFT-RT-001",
//                        "total_cash": 50000.00,
//                        "used_cash": 10000.00,
//                        "cut_of_amount": 500.00,
//                        "available_balance": 39500.00
//            },
//            "postpaid": {
//                "available": false,
//                        "message": "Wallet record not found"
//            }
//        },
//        "assignments": {
//            "assigned": true,
//                    "totalAssignments": 1,
//                    "items": [
//            {
//                "allocationId": 1,
//                    "hierarchyId": "NFT-RT-001",
//                    "product": {
//                "masterRecordFound": true,
//                        "productCode": "PAYOUT",
//                        "name": "Payout",
//                        "active": true
//            },
//                "subProduct": {
//                "masterRecordFound": true,
//                        "subProductCode": "IMPS",
//                        "title": "IMPS",
//                        "walletType": "PREPAID"
//            },
//                "scheme": {
//                "masterRecordFound": true,
//                        "schemeCode": "SCH-001",
//                        "schemeName": "Retailer Payout Scheme",
//                        "active": true
//            }
//            }
//      ]
//        }
//    }
//{
//    "underHierarchy": {
//    "available": true,
//            "rootHierarchyId": "NFT-WL-001",
//            "totalCount": 6,
//            "directChildrenCount": 2,
//            "maximumDepth": 4,
//            "items": [
//    {
//        "hierarchyId": "NFT-SD-001",
//            "hierarchyType": "SD",
//            "parentHierarchyId": "NFT-WL-001",
//            "depth": 1,
//            "relationship": "DIRECT_CHILD",
//            "createdOn": "2026-07-01T10:30:00"
//    },
//    {
//        "hierarchyId": "NFT-SD-002",
//            "hierarchyType": "SD",
//            "parentHierarchyId": "NFT-WL-001",
//            "depth": 1,
//            "relationship": "DIRECT_CHILD",
//            "createdOn": "2026-07-01T11:30:00"
//    },
//    {
//        "hierarchyId": "NFT-PT-001",
//            "hierarchyType": "PT",
//            "parentHierarchyId": "NFT-SD-001",
//            "depth": 2,
//            "relationship": "DESCENDANT",
//            "createdOn": "2026-07-02T10:30:00"
//    },
//    {
//        "hierarchyId": "NFT-DT-001",
//            "hierarchyType": "DT",
//            "parentHierarchyId": "NFT-PT-001",
//            "depth": 3,
//            "relationship": "DESCENDANT",
//            "createdOn": "2026-07-03T10:30:00"
//    },
//    {
//        "hierarchyId": "NFT-RT-001",
//            "hierarchyType": "RT",
//            "parentHierarchyId": "NFT-DT-001",
//            "depth": 4,
//            "relationship": "DESCENDANT",
//            "createdOn": "2026-07-04T10:30:00"
//    }
//    ]
//}
//}
//    }
}
