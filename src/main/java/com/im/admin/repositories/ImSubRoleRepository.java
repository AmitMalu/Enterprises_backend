package com.im.admin.repositories;

import com.im.admin.entities.ImSubRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ImSubRoleRepository extends JpaRepository<ImSubRole, Integer> {
    ImSubRole findByMobileNumber(String mobileNumber);
}

