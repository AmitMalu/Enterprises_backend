package com.im.admin.repositories;

import com.im.admin.entities.BanksDetailss;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BankDetailsRepository extends JpaRepository<BanksDetailss, Long>, JpaSpecificationExecutor<BanksDetailss> {

    List<BanksDetailss> findByHierarchyId(String hierarchyId);

    long countByHierarchyId(String hierarchyId);

}
