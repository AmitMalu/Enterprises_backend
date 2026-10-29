package com.scs.admin.repositories;

import com.scs.admin.entities.RetailerGullakAmount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RetailerGullakAmountRepository extends JpaRepository<RetailerGullakAmount, Long> {
}
