package com.im.admin.repositories;

import com.im.admin.entities.TransactionSequence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface TransactionSequenceRepository extends JpaRepository<TransactionSequence,Long> {
}
