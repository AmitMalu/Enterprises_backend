package com.im.admin.repositories;

import com.im.admin.entities.ImBankMaster;
import com.im.admin.response.BankResponseDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
public interface ImBankMasterRepository extends JpaRepository<ImBankMaster, Long>, JpaSpecificationExecutor<ImBankMaster> {

    @Query("SELECT i FROM ImBankMaster i WHERE i.isActive = true AND i.isBlocked = false")
    List<ImBankMaster> findForDropdown();

    @Query("SELECT new com.im.admin.response.BankResponseDto(b.bankName, b.accountNumber) " +
            "FROM ImBankMaster b WHERE b.isActive = true AND b.isBlocked = false")
    List<BankResponseDto> getActiveBankDetails();

    Optional<ImBankMaster> findByAccountNumber(String accountNumber);
}
