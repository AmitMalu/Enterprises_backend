package com.im.admin.repositories;

import com.im.admin.entities.ChargeMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ChargeMasterRepository extends JpaRepository<ChargeMaster, Long> {

    boolean existsByName(String name);

    Optional<ChargeMaster> findByName(String name);
}
