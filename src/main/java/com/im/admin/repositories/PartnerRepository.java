package com.im.admin.repositories;

import com.im.admin.entities.Partner;
import com.im.admin.entities.Retailer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PartnerRepository extends JpaRepository<Partner, String> {

    //boolean existsByAbbreviation(String abbreviation);

    List<Partner> findByStatus(String status);

    List<Partner> findByParentAccountContaining(String parentType);


    List<Partner> findByStatusIgnoreCase(String status);

    Optional<Partner> findByPartnerId(String partnerId);

    @Query("SELECT p.name FROM Partner p WHERE p.partnerId = :hierarchyId")
    String findNameByHierarchyId(@Param("hierarchyId") String hierarchyId);


    List<Partner> findByParentAccount(String fromId);

    List<Partner> findByParentAccountContainingIgnoreCase(String fromId);

    Partner findByMobileNumber(String mobileNumber);

}

