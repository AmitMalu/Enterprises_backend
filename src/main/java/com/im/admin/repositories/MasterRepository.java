package com.im.admin.repositories;

import com.im.admin.entities.ImMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface MasterRepository extends JpaRepository<ImMaster, Integer> {

    @Query("SELECT imMasterName FROM ImMaster")
    List<String> findAllMasterNames();

    Optional<ImMaster> findByImMasterName(String imMasterName);

}
