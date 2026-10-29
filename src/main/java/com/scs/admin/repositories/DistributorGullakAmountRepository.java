package com.scs.admin.repositories;

import com.scs.admin.entities.DistributorGullakAmount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DistributorGullakAmountRepository extends JpaRepository<DistributorGullakAmount, Long> {

}
