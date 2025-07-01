package com.im.admin.repositories;

import com.im.admin.entities.UserConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserConfigurationRepository extends JpaRepository<UserConfiguration, Integer> {
}
