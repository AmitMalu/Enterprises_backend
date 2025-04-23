package com.im.admin.repositories;

import com.im.admin.entities.AdminPostpaidAmount;
import com.im.admin.entities.AdminPrepaidAmount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AdminPostpaidAmountRepository extends JpaRepository<AdminPostpaidAmount, Long> {

    Optional<AdminPostpaidAmount> findByAdminId_Id(String id);


}
