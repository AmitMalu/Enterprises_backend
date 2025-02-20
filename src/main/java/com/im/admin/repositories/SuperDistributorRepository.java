package com.im.admin.repositories;

import com.im.admin.entities.SuperDistributor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SuperDistributorRepository extends JpaRepository<SuperDistributor, Long> {
}

