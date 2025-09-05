package com.im.admin.repositories;

import com.im.admin.entities.Admin;
import com.im.admin.entities.Enterprises;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface
AdminRepository extends JpaRepository<Admin, String> {

    List<Admin> findByStatus(String status);

    List<Admin> findByStatusIgnoreCase(String status);

    @Query("SELECT a.name FROM Admin a WHERE a.id = :hierarchyId")
    String findNameByHierarchyId(@Param("hierarchyId") String hierarchyId);

    Admin findByMobileNumber(String mobileNumber);

//    List<Admin> findByParentAccount(String fromId);


//    List<Admin> findByParentAccountContaining(String parentType);


    //boolean existsByAbbreviation(String abbreviation);

}


