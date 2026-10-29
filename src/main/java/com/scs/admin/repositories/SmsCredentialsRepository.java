package com.scs.admin.repositories;

import com.scs.admin.entities.SmsCredentialsMaster;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * author abdul.M
 */
public interface SmsCredentialsRepository extends JpaRepository<SmsCredentialsMaster, Long> {

    boolean existsByWhitelabelId(String whitelabelId);

    boolean existsByUsername(String username);

    boolean existsByWhitelabelIdAndIdNot(String whitelabelId, Long id);

    boolean existsByUsernameAndIdNot(String username, Long id);

    Optional<SmsCredentialsMaster> findByWhitelabelId(String whitelabelId);

    boolean existsByWhitelabelIdAndType(String whitelabelId, String type);

    Optional<SmsCredentialsMaster> findByWhitelabelIdAndType(
            String whitelabelId,
            String type
    );



}
