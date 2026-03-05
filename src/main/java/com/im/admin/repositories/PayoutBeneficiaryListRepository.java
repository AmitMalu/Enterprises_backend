package com.im.admin.repositories;


import com.im.admin.entities.PayoutBeneficiaryList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PayoutBeneficiaryListRepository extends JpaRepository<PayoutBeneficiaryList, Long>, JpaSpecificationExecutor<PayoutBeneficiaryList> {

    Optional<PayoutBeneficiaryList>
    findTopByRetailerIdAndAccountNumberOrderByIdDesc(String retailerId,
                                                     String accountNumber);


}
