package com.scs.admin.repositories;

import com.scs.admin.entities.FundReversal;
import com.scs.admin.entities.enums.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FundReversalRepository extends JpaRepository<FundReversal, Long> {

    Optional<FundReversal> findTopByParentHierarchyIdAndChildHierarchyIdAndStatusOrderByIdDesc(
            String parentHierarchyId,
            String childHierarchyId,
            Status status);

}
