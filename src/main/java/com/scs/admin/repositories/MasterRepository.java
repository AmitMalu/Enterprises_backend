package com.scs.admin.repositories;

import com.scs.admin.entities.ScsMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface MasterRepository extends JpaRepository<ScsMaster, Integer> {

    @Query("SELECT scsMasterName FROM ScsMaster")
    List<String> findAllMasterNames();

    Optional<ScsMaster> findByScsMasterName(String scsMasterName);

}
