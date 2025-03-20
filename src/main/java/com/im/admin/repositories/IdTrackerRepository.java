package com.im.admin.repositories;

import com.im.admin.entities.IdTracker;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IdTrackerRepository extends JpaRepository<IdTracker, String> {
}

