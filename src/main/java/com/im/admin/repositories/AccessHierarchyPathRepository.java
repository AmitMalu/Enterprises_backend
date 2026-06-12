package com.im.admin.repositories;

/**
 * @author SHUBHAM KHOPADE
 */
import com.im.admin.entities.AccessHierarchyPath;
import com.im.admin.entities.AccessHierarchyPathId;
import org.springframework.data.jpa.repository.JpaRepository;
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

}
