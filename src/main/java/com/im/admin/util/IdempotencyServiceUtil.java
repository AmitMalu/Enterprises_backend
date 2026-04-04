package com.im.admin.util;

import com.im.admin.entities.WebhookIdempotency;
import com.im.admin.repositories.WebhookIdempotencyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * @author SHUBHAM KHOPADE
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class IdempotencyServiceUtil {

    private final WebhookIdempotencyRepository repository;

    public boolean tryAcquire(String idempotencyKey) {

        try {
            WebhookIdempotency entity = new WebhookIdempotency();
            entity.setIdempotencyId(idempotencyKey);
            entity.setCreatedAt(LocalDateTime.now());

            repository.save(entity);

            return true;

        } catch (DataIntegrityViolationException ex) {

            log.warn("Duplicate webhook detected for key: {}", idempotencyKey);
            return false; // duplicate → already processed
        }
    }
}
