package com.im.admin.repositories;

import com.im.admin.entities.Enterprises;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EnterprisesRepository extends JpaRepository<Enterprises, String> {

}


