package com.im.admin.repositories;

/**
 * @author SHUBHAM KHOPADE
 */
import com.im.admin.entities.AccessHierarchyNode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AccessHierarchyNodeRepository
        extends JpaRepository<AccessHierarchyNode, String> {
}
