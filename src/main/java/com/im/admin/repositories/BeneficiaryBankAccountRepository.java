package com.im.admin.repositories;


import com.im.admin.entities.BeneficiaryBankAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BeneficiaryBankAccountRepository extends JpaRepository<BeneficiaryBankAccount, Long>, JpaSpecificationExecutor<BeneficiaryBankAccount> {

    Optional<BeneficiaryBankAccount>
    findTopByRetailerIdAndAccountNumberOrderByIdDesc(String retailerId,
                                                     String accountNumber);

    BeneficiaryBankAccount findByIfscCode(String ifscCode);

}
