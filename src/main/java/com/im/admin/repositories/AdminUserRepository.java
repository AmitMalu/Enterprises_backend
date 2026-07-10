package com.im.admin.repositories;

import com.im.admin.entities.AdminUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface AdminUserRepository extends JpaRepository<AdminUser,String>, JpaSpecificationExecutor<AdminUser> {

    Optional<AdminUser> findById(String id);

}
