package com.scs.admin.repositories;

import com.scs.admin.entities.VimoPayBankList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * @author SHUBHAM KHOPADE
 */

@Repository
public interface VimoPayBankListRepository extends JpaRepository<VimoPayBankList, Long> {

    Optional<VimoPayBankList> findByVendor_IdAndBankName(
            String vendorId,
            String bankName
    );
}
