package com.im.admin.repositories;

import com.im.admin.entities.BbpsProviderList;
import com.im.admin.entities.DmtBankList;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DmtBankListRepository extends JpaRepository<DmtBankList, Integer> {

}
