package com.im.admin.repositories;


import com.im.admin.entities.SchemeAllocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    Optional<SchemeAllocation> findByHierarchyIdAndProductCodeAndSubProductCode(
            String hierarchyId, String productCode, String subProductCode);

    boolean existsByHierarchyIdAndSchemeCodeAndProductCodeAndSubProductCode(
            String hierarchyId, String schemeCode, String productCode, String subProductCode);

    Optional<SchemeAllocation> findByHierarchyIdAndSubProductCode(String hierarchyId, String subProductCode);

    List<SchemeAllocation>
    findAllByHierarchyId(
            String hierarchyId
    );

    List<SchemeAllocation> findAllByHierarchyIdOrderByIdAsc(
            String hierarchyId
    );

    /**
     * Deletes the subproduct allocation from:
     *
     * 1. The old White Label itself
     * 2. Every hierarchy below that White Label
     *
     * access_hierarchy_path contains depth = 0 for the White Label itself,
     * therefore the White Label allocation is also removed.
     */
    @Modifying
    @Query(
            value = """
                    DELETE FROM scheme_allocations
                    WHERE sub_product_code = :subProductCode
                      AND hierarchy_id IN (
                          SELECT descendant_id
                          FROM access_hierarchy_path
                          WHERE ancestor_id = :whiteLabelId
                      )
                    """,
            nativeQuery = true
    )
    int deleteSubProductAllocationsUnderWhiteLabel(
            @Param("whiteLabelId") String whiteLabelId,
            @Param("subProductCode") String subProductCode
    );

}

