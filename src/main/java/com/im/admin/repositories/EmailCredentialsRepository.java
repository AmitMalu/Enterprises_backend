package com.im.admin.repositories;

import com.im.admin.entities.EmailCredentialsMaster;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmailCredentialsRepository extends JpaRepository<EmailCredentialsMaster, Long> {

    Optional<EmailCredentialsMaster> findByAdminId(String adminId);

    Optional<EmailCredentialsMaster> findByWhitelabelId(String whitelabelId);

    boolean existsByAdminId(String adminId);


    boolean existsByWhitelabelId(String whitelabelId);

    boolean existsByEmail(String email);

    // For update
    boolean existsByWhitelabelIdAndIdNot(String whitelabelId, Long id);

    boolean existsByEmailAndIdNot(String email, Long id);
}
