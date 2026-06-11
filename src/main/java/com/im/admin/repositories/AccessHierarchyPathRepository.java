package com.im.admin.repositories;

/**
 * @author SHUBHAM KHOPADE
 */
import com.im.admin.entities.AccessHierarchyPath;
import com.im.admin.entities.AccessHierarchyPathId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AccessHierarchyPathRepository
        extends JpaRepository<AccessHierarchyPath, AccessHierarchyPathId> {

    List<AccessHierarchyPath> findByIdAncestorId(String ancestorId);

    List<AccessHierarchyPath> findByIdDescendantId(String descendantId);
}
