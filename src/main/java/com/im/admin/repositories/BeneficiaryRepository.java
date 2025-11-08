package com.im.admin.repositories;

import com.im.admin.entities.DmtBeneficiary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BeneficiaryRepository extends JpaRepository<DmtBeneficiary, Long> {
    DmtBeneficiary findByBeneId(Long beneId);
}
