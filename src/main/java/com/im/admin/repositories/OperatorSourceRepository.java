package com.im.admin.repositories;


import com.im.admin.entities.OperatorSource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface OperatorSourceRepository extends JpaRepository<OperatorSource, Long> {


}

