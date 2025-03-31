package com.im.admin.repositories;

import com.im.admin.entities.AdminPostpaidAmount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AdminPostpaidAmountRepository extends JpaRepository<AdminPostpaidAmount, Long> {

}
