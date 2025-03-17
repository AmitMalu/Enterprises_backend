package com.im.admin.repositories;


import com.im.admin.entities.SchemeAllocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SchemeAllocationRepository extends JpaRepository<SchemeAllocation, Long> {
}

