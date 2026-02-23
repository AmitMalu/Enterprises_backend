package com.im.admin.repositories;

import com.im.admin.entities.DmtBeneficiary;
import com.im.admin.entities.DmtRemitter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BeneficiaryRepository extends JpaRepository<DmtBeneficiary, Long> {
    DmtBeneficiary findByBeneId(Long beneId);
    List<DmtBeneficiary> findByRemitter(DmtRemitter remitter);

    Optional<DmtBeneficiary> findByRemitterAndBeneId(
            DmtRemitter remitter,
            Long beneId
    );

    List<DmtBeneficiary> findByRemitterOrderByIdDesc(DmtRemitter remitter);

}
