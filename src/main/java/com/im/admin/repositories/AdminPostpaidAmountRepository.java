package com.im.admin.repositories;

import com.im.admin.entities.AdminPostpaidAmount;
import com.im.admin.entities.AdminPrepaidAmount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface AdminPostpaidAmountRepository extends JpaRepository<AdminPostpaidAmount, Long> {

    Optional<AdminPostpaidAmount> findByAdminId_Id(String id);

    @Query("""
    SELECT COALESCE(SUM(a.availableBalance), 0)
    FROM AdminPostpaidAmount a
    WHERE a.adminId.id IN :ids
""")
    BigDecimal sumAvailableBalanceByAdmins(@Param("ids") List<String> ids);


}
