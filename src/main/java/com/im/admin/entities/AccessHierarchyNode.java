package com.im.admin.entities;

/**
 * @author SHUBHAM KHOPADE
 */
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "access_hierarchy_node",
        indexes = {
                @Index(name = "idx_ahn_parent", columnList = "parent_hierarchy_id"),
                @Index(name = "idx_ahn_type", columnList = "hierarchy_type")
        }
)
@Getter
@Setter
public class AccessHierarchyNode {

    @Id
    @Column(name = "hierarchy_id", nullable = false, length = 20)
    private String hierarchyId;

    @Column(name = "hierarchy_type", nullable = false, length = 10)
    private String hierarchyType;

    @Column(name = "parent_hierarchy_id", length = 20)
    private String parentHierarchyId;

    @Column(name = "created_on")
    private LocalDateTime createdOn;
}
