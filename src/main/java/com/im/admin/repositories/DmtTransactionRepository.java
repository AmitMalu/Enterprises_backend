package com.im.admin.repositories;


import com.im.admin.entities.DmtTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DmtTransactionRepository extends JpaRepository<DmtTransaction, Long> {
}
