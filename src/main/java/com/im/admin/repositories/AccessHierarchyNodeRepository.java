package com.im.admin.repositories;

/**
 * @author SHUBHAM KHOPADE
 */
import com.im.admin.entities.AccessHierarchyNode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccessHierarchyNodeRepository
        extends JpaRepository<AccessHierarchyNode, String> {


    @Query("""
            select n.parentHierarchyId
            from AccessHierarchyNode n
            where n.hierarchyId = :hierarchyId
            """)
    Optional<String> findParentHierarchyId(
            @Param("hierarchyId")
            String hierarchyId
    );

    List<AccessHierarchyNode> findByParentHierarchyId(String parentId);

    Optional<AccessHierarchyNode> findByParentHierarchyIdIsNull();

}
