package com.im.admin.repositories;

import com.im.admin.entities.Distributor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DistributorRepository extends JpaRepository<Distributor, String> {
}

