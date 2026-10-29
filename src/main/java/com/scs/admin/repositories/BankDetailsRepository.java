package com.scs.admin.repositories;

import com.scs.admin.entities.BanksDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BankDetailsRepository extends JpaRepository<BanksDetails, Long>, JpaSpecificationExecutor<BanksDetails> {

    List<BanksDetails> findByHierarchyId(String hierarchyId);

    long countByHierarchyId(String hierarchyId);

    List<BanksDetails> findByHierarchyIdAndIsVerifiedTrue(String hierarchyId);

}
