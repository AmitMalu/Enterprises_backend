package com.im.admin.repositories;

import com.im.admin.entities.WhiteLabelUser;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * @author SHUBHAM KHOPADE
 */
@Repository
public interface WhiteLabelUserRepository extends
        JpaRepository<WhiteLabelUser, String>,
        JpaSpecificationExecutor<WhiteLabelUser> {

    boolean existsByMobileNumber(String mobileNumber);

    boolean existsByUsername(String username);

    boolean existsByEmailId(String emailId);

    @NotNull Optional<WhiteLabelUser> findById(@NotNull String id);
}
