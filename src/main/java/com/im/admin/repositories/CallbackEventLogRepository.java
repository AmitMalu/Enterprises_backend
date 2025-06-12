package com.im.admin.repositories;

import com.im.admin.entities.CallbackEventLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CallbackEventLogRepository extends JpaRepository<CallbackEventLog, Integer> {

}
