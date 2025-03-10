package com.im.admin.repositories;

import com.im.admin.entities.HierarchyMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface HierarchyMasterRepository extends JpaRepository<HierarchyMaster, Long> {

    Set<HierarchyMaster> findByNameIn(List<String> names);
}

