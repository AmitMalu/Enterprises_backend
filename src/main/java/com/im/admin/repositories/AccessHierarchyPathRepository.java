package com.im.admin.repositories;

/**
 * @author SHUBHAM KHOPADE
 */

import com.im.admin.entities.AccessHierarchyPath;
import com.im.admin.entities.AccessHierarchyPathId;
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

//    @Query("""
//SELECT p.descendantId
//FROM AccessHierarchyPath p
//WHERE p.ancestorId=:ancestorId
//AND p.depth>0
//""")
//    List<String> findDescendantIds(
//            @Param("ancestorId")
//            String ancestorId
//    );

}
