package com.im.admin.repositories;

import com.im.admin.entities.LoginLogs;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoginLogsRepository extends JpaRepository<LoginLogs,Integer> {

}
