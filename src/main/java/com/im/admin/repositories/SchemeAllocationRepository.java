package com.im.admin.repositories;


import com.im.admin.entities.SchemeAllocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SchemeAllocationRepository extends JpaRepository<SchemeAllocation, Long> {

    List<SchemeAllocation> findByHierarchyId(String hierarchyId);

}

