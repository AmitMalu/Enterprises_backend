package com.scs.admin.repositories;

import com.scs.admin.entities.IdTracker;
import com.scs.admin.entities.IdTrackerKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IdTrackerRepository extends JpaRepository<IdTracker, IdTrackerKey> {
}

