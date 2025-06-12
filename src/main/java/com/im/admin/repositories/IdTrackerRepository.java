package com.im.admin.repositories;

import com.im.admin.entities.IdTracker;
import com.im.admin.entities.IdTrackerKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IdTrackerRepository extends JpaRepository<IdTracker, IdTrackerKey> {
}

