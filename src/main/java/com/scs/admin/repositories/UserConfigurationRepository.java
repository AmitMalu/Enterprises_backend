package com.scs.admin.repositories;

import com.scs.admin.entities.UserConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserConfigurationRepository extends JpaRepository<UserConfiguration, Integer> {
}
