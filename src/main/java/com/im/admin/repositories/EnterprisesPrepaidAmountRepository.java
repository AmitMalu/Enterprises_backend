package com.im.admin.repositories;

import com.im.admin.entities.Enterprises;
import com.im.admin.entities.EnterprisesPrepaidAmount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EnterprisesPrepaidAmountRepository extends JpaRepository<EnterprisesPrepaidAmount, Long> {

    void deleteByEnterprise(Enterprises enterprise);
}
