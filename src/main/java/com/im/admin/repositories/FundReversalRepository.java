package com.im.admin.repositories;

import com.im.admin.entities.FundReversal;
import com.im.admin.entities.enums.ConstantStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FundReversalRepository extends JpaRepository<FundReversal, Long> {

    Optional<FundReversal> findTopByParentHierarchyIdAndChildHierarchyIdAndStatusOrderByIdDesc(
            String parentHierarchyId,
            String childHierarchyId,
            ConstantStatus status);

}
