package com.im.admin.repositories;

import com.im.admin.entities.UserDetails;
import com.im.admin.entities.UserDetailsId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserDetailsRepository extends JpaRepository<UserDetails, UserDetailsId> {

    Optional<UserDetails> findByMobileNumber(String mobileNumber);

    Optional<UserDetails> findByEmailId(String email);

    Optional<UserDetails> findFirstByHierarchyId(String hierarchyId);

    List<UserDetails> findByHierarchyId(String hierarchyId);

    @Query("SELECT u FROM UserDetails u WHERE u.userStatus = 'Active' AND u.hierarchyId LIKE %:keyword%")
    Page<UserDetails> searchActiveUsersByHierarchyId(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT u.hierarchyId FROM UserDetails u WHERE u.hierarchyCode = :code")
    List<String> findHierarchyIdsByHierarchyCode(@Param("code") String code);


}
