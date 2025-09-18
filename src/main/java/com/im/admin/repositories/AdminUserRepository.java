package com.im.admin.repositories;

import com.im.admin.entities.AdminUser;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminUserRepository extends JpaRepository<AdminUser,String> {

}
