package com.im.admin.repositories;

import com.im.admin.entities.DmtRemitter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RemitterRepository extends JpaRepository<DmtRemitter, Long> {
    Optional<DmtRemitter> findByMobile(String mobile);
}

