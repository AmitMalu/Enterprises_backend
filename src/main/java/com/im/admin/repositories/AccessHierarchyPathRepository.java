package com.im.admin.repositories;

/**
 * @author SHUBHAM KHOPADE
 */

import com.im.admin.entities.AccessHierarchyPath;
import com.im.admin.entities.AccessHierarchyPathId;
import com.im.admin.util.UnderHierarchyProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AccessHierarchyPathRepository
        extends JpaRepository<AccessHierarchyPath, AccessHierarchyPathId> {

    List<AccessHierarchyPath> findByIdDescendantId(String descendantId);

    @Query("""
            select p.id.descendantId
            from AccessHierarchyPath p
            where p.id.ancestorId = :ancestorId
            """)
    List<String> findDescendantsByAncestor(
            @Param("ancestorId") String ancestorId);

    @Query("""
            select p.id.descendantId
            from AccessHierarchyPath p
            where p.id.ancestorId = :ancestorId
            and p.id.descendantId like '%-RT-%'
            """)
    List<String> findRetailerDescendants(
            @Param("ancestorId") String ancestorId);

    @Query("""
            select p.id.descendantId
            from AccessHierarchyPath p
            where p.id.ancestorId = :ancestorId
            and p.id.descendantId like concat('%-', :type, '-%')
            """)
    List<String> findDescendantsByAncestorAndType(
            @Param("ancestorId") String ancestorId,
            @Param("type") String type);

    List<AccessHierarchyPath>
    findByIdAncestorId(
            String ancestorId
    );

    boolean existsById(AccessHierarchyPathId id);

    @Query("""
            SELECT p
            FROM AccessHierarchyPath p
            WHERE p.id.descendantId=:descendantId
            ORDER BY p.depth
            """)
    List<AccessHierarchyPath> findAllByDescendant(
            @Param("descendantId")
            String descendantId
    );

    @Modifying
    @Query("""
            DELETE
            FROM AccessHierarchyPath p
            WHERE p.id.descendantId=:descendantId
            AND p.depth>0
            """)
    void deleteByDescendantExcludingSelf(
            @Param("descendantId")
            String descendantId
    );

    @Query("""
        SELECT p.id.ancestorId
        FROM AccessHierarchyPath p
        WHERE p.id.descendantId = :descendant
        ORDER BY p.depth
    """)
    List<String> findAncestors(
            @Param("descendant")
            String descendant
    );

    @Query(value = """
        SELECT COUNT(*)
        FROM access_hierarchy_path
        WHERE ancestor_id = :ancestorId
          AND descendant_id = :descendantId
        """, nativeQuery = true)
    Long countHierarchyPath(
            @Param("ancestorId") String ancestorId,
            @Param("descendantId") String descendantId
    );

    @Query("""
            SELECT
                node.hierarchyId AS hierarchyId,
                node.hierarchyType AS hierarchyType,
                node.parentHierarchyId AS parentHierarchyId,
                node.createdOn AS createdOn,
                path.depth AS depth
            FROM AccessHierarchyPath path
            JOIN AccessHierarchyNode node
                ON node.hierarchyId = path.id.descendantId
            WHERE path.id.ancestorId = :ancestorId
              AND path.depth > 0
            ORDER BY path.depth ASC, node.hierarchyId ASC
            """)
    List<UnderHierarchyProjection> findAllDescendants(
            @Param("ancestorId") String ancestorId
    );

    @Query(
            value = """
                    SELECT COUNT(*)
                    FROM access_hierarchy_path
                    WHERE ancestor_id = :whiteLabelId
                      AND descendant_id = :hierarchyId
                    """,
            nativeQuery = true
    )
    long countHierarchyUnderWhiteLabel(
            @Param("whiteLabelId") String whiteLabelId,
            @Param("hierarchyId") String hierarchyId
    );

}
