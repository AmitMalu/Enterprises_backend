package com.im.admin.repositories;

import com.im.admin.entities.ImSubRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface ImSubRoleRepository extends JpaRepository<ImSubRole, Integer> {
    ImSubRole findByMobileNumber(String mobileNumber);

    @Modifying
    @Transactional
    @Query("DELETE FROM ImSubRole r WHERE r.mobileNumber = :mobileNumber")
    void deleteByMobileNumber(@Param("mobileNumber") String mobileNumber);
}

