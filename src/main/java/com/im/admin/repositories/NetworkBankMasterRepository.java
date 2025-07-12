package com.im.admin.repositories;

import com.im.admin.entities.NetworkBankMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface NetworkBankMasterRepository extends JpaRepository<NetworkBankMaster,Long>, JpaSpecificationExecutor<NetworkBankMaster>  {
}
