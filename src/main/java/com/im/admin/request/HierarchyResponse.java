package com.im.admin.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Map;

/**
 * @author SHUBHAM KHOPADE
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class HierarchyResponse {

    private TreeNode root;
    private List<TreeNode> allNodes;
    private Map<String, Integer> depthMap;
    private Integer totalNodes;

}
