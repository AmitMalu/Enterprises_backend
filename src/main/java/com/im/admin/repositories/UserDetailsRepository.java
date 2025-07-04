package com.im.admin.repositories;

import com.im.admin.entities.UserDetails;
import com.im.admin.entities.UserDetailsId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserDetailsRepository extends JpaRepository<UserDetails, UserDetailsId> {

    Optional<UserDetails> findByMobileNumber(String mobileNumber);

    Optional<UserDetails> findByEmailId(String email);

    Optional<UserDetails> findFirstByHierarchyId(String hierarchyId);

    List<UserDetails> findByHierarchyId(String hierarchyId);
}
