package com.scs.admin.repositories;

import com.scs.admin.entities.ScsBankMaster;
import com.scs.admin.response.BankResponseDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
public interface ScsBankMasterRepository extends JpaRepository<ScsBankMaster, Long>, JpaSpecificationExecutor<ScsBankMaster> {

    @Query("SELECT i FROM ScsBankMaster i WHERE i.isActive = true AND i.isBlocked = false")
    List<ScsBankMaster> findForDropdown();

    @Query("SELECT new com.scs.admin.response.BankResponseDto(b.bankName, b.accountNumber) " +
            "FROM ScsBankMaster b WHERE b.isActive = true AND b.isBlocked = false")
    List<BankResponseDto> getActiveBankDetails();

    Optional<ScsBankMaster> findByAccountNumber(String accountNumber);

    List<ScsBankMaster> findByWhitelabelIdAndIsActiveTrue(String whitelabelId);
}
