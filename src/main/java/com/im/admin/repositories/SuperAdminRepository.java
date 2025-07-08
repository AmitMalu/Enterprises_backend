package com.im.admin.repositories;

import com.im.admin.entities.SuperAdmin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SuperAdminRepository extends JpaRepository<SuperAdmin, String> {
    List<SuperAdmin> findByStatusIgnoreCase(String status);


}

