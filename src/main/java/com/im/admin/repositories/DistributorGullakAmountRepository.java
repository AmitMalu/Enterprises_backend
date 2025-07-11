package com.im.admin.repositories;

import com.im.admin.entities.DistributorGullakAmount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DistributorGullakAmountRepository extends JpaRepository<DistributorGullakAmount, Long> {

}
