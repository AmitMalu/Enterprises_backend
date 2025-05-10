package com.im.admin.repositories;

import com.im.admin.entities.Enterprises;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EnterprisesRepository extends JpaRepository<Enterprises, String> {

    //find(String id);

    List<Enterprises> findByStatus(String status);


}


