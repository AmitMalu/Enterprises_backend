package com.im.admin.repositories;

import com.im.admin.entities.WebhookIdempotency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * @author SHUBHAM KHOPADE
 */
@Repository
public interface WebhookIdempotencyRepository
        extends JpaRepository<WebhookIdempotency, Long> {

    boolean existsByIdempotencyId(String idempotencyId);

    Optional<WebhookIdempotency> findByIdempotencyId(String idempotencyId);
}
