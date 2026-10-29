package com.scs.admin.repositories;

import com.scs.admin.entities.ScsSubRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface ScsSubRoleRepository extends JpaRepository<ScsSubRole, Integer> {
    ScsSubRole findByMobileNumber(String mobileNumber);

    @Modifying
    @Transactional
    @Query("DELETE FROM ScsSubRole r WHERE r.mobileNumber = :mobileNumber")
    void deleteByMobileNumber(@Param("mobileNumber") String mobileNumber);
}

