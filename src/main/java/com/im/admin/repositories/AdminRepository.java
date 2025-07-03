package com.im.admin.repositories;

import com.im.admin.entities.Admin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdminRepository extends JpaRepository<Admin, String> {

    List<Admin> findByStatus(String status);

//    List<Admin> findByParentAccountContaining(String parentType);


    //boolean existsByAbbreviation(String abbreviation);

}


