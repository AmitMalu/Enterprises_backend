package com.im.admin.repositories;


import com.im.admin.entities.SchemeAllocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SchemeAllocationRepository extends JpaRepository<SchemeAllocation, Long> {

    List<SchemeAllocation> findByHierarchyId(String hierarchyId);

    Optional<SchemeAllocation> findBySchemeCodeAndProductCodeAndSubProductCodeAndHierarchyId(
            String schemeCode,
            String productCode,
            String subProductCode,
            String hierarchyId
    );

}

