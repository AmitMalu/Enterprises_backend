package com.im.admin.repositories;

import com.im.admin.entities.ImSubMaster;
import com.im.admin.entities.ImSubMasterId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ImSubMasterRepository extends JpaRepository<ImSubMaster, ImSubMasterId> {

    List<ImSubMaster> findByImMasterId(String masterId);

}
