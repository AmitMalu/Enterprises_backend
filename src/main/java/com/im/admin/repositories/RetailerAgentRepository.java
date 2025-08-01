package com.im.admin.repositories;

import com.im.admin.entities.RetailerAgent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RetailerAgentRepository extends JpaRepository<RetailerAgent,Long> {
    long countByParentAccount(String retailerId);

}
