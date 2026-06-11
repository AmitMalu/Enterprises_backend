package com.im.admin.entities;

/**
 * @author SHUBHAM KHOPADE
 */
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
        name = "access_hierarchy_path",
        indexes = {
                @Index(name = "idx_ahp_ancestor", columnList = "ancestor_id"),
                @Index(name = "idx_ahp_descendant", columnList = "descendant_id"),
                @Index(name = "idx_ahp_depth", columnList = "depth")
        }
)
@Getter
@Setter
public class AccessHierarchyPath {

    @EmbeddedId
    private AccessHierarchyPathId id;

    @Column(name = "depth", nullable = false)
    private Integer depth;
}
