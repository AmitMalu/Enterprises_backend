package com.im.admin.repositories;

import com.im.admin.entities.NetworkBankMaster;
import com.im.admin.response.NetworkBankDropdownResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NetworkBankMasterRepository extends JpaRepository<NetworkBankMaster,Long>, JpaSpecificationExecutor<NetworkBankMaster>  {


    @Query("SELECT new com.im.admin.response.NetworkBankDropdownResponse(b.id, b.bankName) FROM NetworkBankMaster b WHERE b.isActive = true")
    List<NetworkBankDropdownResponse> getActiveBankDropdown();

    Optional<NetworkBankMaster> findByIfscCodeIgnoreCase(String ifscCode);

}
