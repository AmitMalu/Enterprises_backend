package com.im.admin.repositories;

import com.im.admin.entities.AepsTwoFa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface Aeps2FaRepository extends JpaRepository<AepsTwoFa, Long> {
    Optional<AepsTwoFa> findTopByRetailerIdOrderByIdDesc(String retailerId);

}

