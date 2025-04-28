package com.im.admin.repositories;

import com.im.admin.entities.Enterprises;
import com.im.admin.entities.EnterprisesPostpaidAmount;
import com.im.admin.entities.EnterprisesPrepaidAmount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EnterprisesPrepaidAmountRepository extends JpaRepository<EnterprisesPrepaidAmount, Long> {

    void deleteByEnterprise(Enterprises enterprise);

    Optional<EnterprisesPrepaidAmount> findByEnterprise_EnterpriseId(String enterprisesId);

}
