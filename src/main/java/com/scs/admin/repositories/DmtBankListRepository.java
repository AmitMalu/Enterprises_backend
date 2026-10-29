package com.scs.admin.repositories;

import com.scs.admin.entities.BbpsProviderList;
import com.scs.admin.entities.DmtBankList;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DmtBankListRepository extends JpaRepository<DmtBankList, Integer> {

}
