package com.im.admin.request;

import com.im.admin.dto.SchemeAllocationDetailsDTO;
import com.im.admin.entities.SchemeAllocation;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author SHUBHAM KHOPADE
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Slf4j
public class TreeNode {

    private String id;
    private String type;
    private String parentId;
    private String mobileNumber;
    private String otp;
    private String createdOn;
    private Integer depth;
    private List<TreeNode> children;
    private BigDecimal prepaidBalance;
    private BigDecimal postpaidBalance;

    // ===== CLEAN SCHEME STRUCTURE =====
    private List<ProductSchemeGroup> products;  // Grouped by product with schemes
    private boolean hasSchemes;

    public TreeNode(String id, String type, String parentId, String createdOn) {
        this.id = id;
        this.type = type;
        this.parentId = parentId;
        this.createdOn = createdOn;
        this.children = new ArrayList<>();
    }

    public void addChild(TreeNode child) {
        if (this.children == null) {
            this.children = new ArrayList<>();
        }
        if (child != null) {
            this.children.add(child);
        } else {
            log.warn("Attempted to add null child to node {}", this.id);
        }
    }

    public List<TreeNode> getChildren() {
        if (this.children == null) {
            this.children = new ArrayList<>();
        }
        return this.children;
    }

    public void setChildren(List<TreeNode> children) {
        this.children = children != null ? children : new ArrayList<>();
    }
}
