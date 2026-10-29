package com.scs.admin.repositories;

import com.scs.admin.entities.DmtBeneficiary;
import com.scs.admin.entities.DmtRemitter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BeneficiaryRepository extends JpaRepository<DmtBeneficiary, Long> {
    DmtBeneficiary findByBeneId(String beneId);
    List<DmtBeneficiary> findByRemitter(DmtRemitter remitter);

    Optional<DmtBeneficiary> findByRemitterAndBeneId(
            DmtRemitter remitter,
            String beneId
    );

    List<DmtBeneficiary> findByRemitterOrderByIdDesc(DmtRemitter remitter);

    Optional<DmtBeneficiary> findByRemitterAndAccountNo(
            DmtRemitter remitter,
            String accountNo
    );

    Optional<DmtBeneficiary> findByBeneIdAndRemitter_Mobile(String beneId, String mobile);

    Optional<DmtBeneficiary> findByAccountNo(String accountNo);

}
