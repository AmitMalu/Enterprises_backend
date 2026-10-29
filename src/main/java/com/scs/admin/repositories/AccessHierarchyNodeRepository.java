package com.scs.admin.repositories;

/**
 * @author SHUBHAM KHOPADE
 */
import com.scs.admin.entities.AccessHierarchyNode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccessHierarchyNodeRepository
        extends JpaRepository<AccessHierarchyNode, String> {


    Optional<AccessHierarchyNode> findByHierarchyId(String hierarchyId);

    /*
     * Used for AD and AU.
     *
     * Returns every hierarchy node having the requested type.
     */
    @Query("""
            SELECT node
            FROM AccessHierarchyNode node
            WHERE UPPER(node.hierarchyType) = UPPER(:hierarchyType)
            ORDER BY node.hierarchyId ASC
            """)
    List<AccessHierarchyNode> findAllByHierarchyType(
            @Param("hierarchyType") String hierarchyType
    );

    /*
     * Used for WL, WLAU, SD, PT, DT and RT.
     *
     * Returns only matching hierarchy nodes under the given
     * effective hierarchy tree.
     */
    @Query("""
            SELECT node
            FROM AccessHierarchyNode node
            WHERE UPPER(node.hierarchyType) = UPPER(:hierarchyType)
              AND node.hierarchyId IN (
                    SELECT path.id.descendantId
                    FROM AccessHierarchyPath path
                    WHERE path.id.ancestorId = :ancestorHierarchyId
              )
            ORDER BY node.hierarchyId ASC
            """)
    List<AccessHierarchyNode> findByTypeUnderHierarchy(
            @Param("ancestorHierarchyId")
            String ancestorHierarchyId,

            @Param("hierarchyType")
            String hierarchyType
    );

}
