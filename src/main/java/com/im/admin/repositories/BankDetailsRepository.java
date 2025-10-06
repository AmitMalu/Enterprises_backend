package com.im.admin.repositories;

import com.im.admin.entities.BanksDetails;
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
