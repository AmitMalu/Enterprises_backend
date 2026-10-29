package com.scs.admin.repositories;

import com.scs.admin.entities.TransactionSequence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface TransactionSequenceRepository extends JpaRepository<TransactionSequence,Long> {
}
