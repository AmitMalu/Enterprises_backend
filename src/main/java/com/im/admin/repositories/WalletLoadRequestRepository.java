package com.im.admin.repositories;

import com.im.admin.entities.WalletLoadRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WalletLoadRequestRepository extends JpaRepository<WalletLoadRequest, Long> {


}
