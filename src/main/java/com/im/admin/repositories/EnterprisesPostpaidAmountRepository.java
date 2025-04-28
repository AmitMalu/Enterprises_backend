package com.im.admin.repositories;

import com.im.admin.entities.Enterprises;
import com.im.admin.entities.EnterprisesPostpaidAmount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EnterprisesPostpaidAmountRepository extends JpaRepository<EnterprisesPostpaidAmount, Long> {

    void deleteByEnterprise(Enterprises enterprise);

    Optional<EnterprisesPostpaidAmount> findByEnterprise_EnterpriseId(String enterprisesId);
}
