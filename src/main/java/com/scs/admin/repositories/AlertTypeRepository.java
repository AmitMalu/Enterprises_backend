package com.scs.admin.repositories;

import com.scs.admin.entities.AlertType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlertTypeRepository
        extends JpaRepository<AlertType, Long> {
}
