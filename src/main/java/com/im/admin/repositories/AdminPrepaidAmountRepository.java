package com.im.admin.repositories;

import com.im.admin.entities.AdminPrepaidAmount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AdminPrepaidAmountRepository extends JpaRepository<AdminPrepaidAmount, Long> {

    Optional<AdminPrepaidAmount> findByAdminId_Id(String id);
}
