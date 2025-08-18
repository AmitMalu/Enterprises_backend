package com.im.admin.repositories;

import com.im.admin.entities.RetailerUser;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RetailerUserRepository extends JpaRepository<RetailerUser,String> {
    long countByParentAccount(String retailerId);

}
