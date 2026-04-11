package com.im.admin.repositories;

import com.im.admin.entities.AlertType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlertTypeRepository
        extends JpaRepository<AlertType, Long> {
}
